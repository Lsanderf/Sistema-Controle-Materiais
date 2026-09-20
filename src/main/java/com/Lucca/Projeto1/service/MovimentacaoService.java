package com.Lucca.Projeto1.service;

import com.Lucca.Projeto1.dto.movimentacao.MovimentacaoRequest;
import com.Lucca.Projeto1.dto.movimentacao.MovimentacaoResponse;
import com.Lucca.Projeto1.dto.movimentacao.EstornoMovimentacaoRequest;
import com.Lucca.Projeto1.exception.RecursoNaoEncontradoException;
import com.Lucca.Projeto1.exception.RegraNegocioException;
import com.Lucca.Projeto1.mapper.MovimentacaoMapper;
import com.Lucca.Projeto1.model.Contrato;
import com.Lucca.Projeto1.model.Funcionario;
import com.Lucca.Projeto1.model.Material;
import com.Lucca.Projeto1.model.Movimentacao;
import com.Lucca.Projeto1.model.TipoMovimentacao;
import com.Lucca.Projeto1.model.Usuario;
import com.Lucca.Projeto1.repository.ContratoRepository;
import com.Lucca.Projeto1.repository.FuncionarioRepository;
import com.Lucca.Projeto1.repository.MaterialRepository;
import com.Lucca.Projeto1.repository.MovimentacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MovimentacaoService {

    private static final int LIMITE_QUANTIDADE_OPERACAO = 10000;

    private final MovimentacaoRepository movimentacaoRepository;
    private final ContratoRepository contratoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final MaterialRepository materialRepository;
    private final UsuarioAutenticadoService usuarioAutenticadoService;
    private final ComprovanteMovimentacaoService comprovanteService;
    private final EvidenciaMovimentacaoService evidenciaService;
    private final ImagemEvidenciaValidator imagemValidator;

    public MovimentacaoService(
            MovimentacaoRepository movimentacaoRepository,
            FuncionarioRepository funcionarioRepository,
            ContratoRepository contratoRepository,
            MaterialRepository materialRepository,
            UsuarioAutenticadoService usuarioAutenticadoService,
            ComprovanteMovimentacaoService comprovanteService,
            EvidenciaMovimentacaoService evidenciaService,
            ImagemEvidenciaValidator imagemValidator
    ) {
        this.movimentacaoRepository = movimentacaoRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.contratoRepository = contratoRepository;
        this.materialRepository = materialRepository;
        this.usuarioAutenticadoService = usuarioAutenticadoService;
        this.comprovanteService = comprovanteService;
        this.evidenciaService = evidenciaService;
        this.imagemValidator = imagemValidator;
    }

    @Transactional
    public MovimentacaoResponse registrarMovimentacao(
            MovimentacaoRequest request,
            MultipartFile assinatura,
            MultipartFile foto,
            String idempotencyKey
    ) {
        String chaveIdempotencia =
                normalizarIdempotencyKey(idempotencyKey);

        validarQuantidade(request.getQuantidade());
        validarTipoMovimentacaoComum(request.getTipo());
        if (foto != null && request.getTipo() != TipoMovimentacao.DEVOLUCAO) {
            throw new RegraNegocioException("Foto do material é permitida apenas na devolução");
        }
        var assinaturaValidada = imagemValidator.validar(assinatura, true);
        var fotoValidada = foto == null ? null : imagemValidator.validar(foto, false);
        Usuario usuarioAutenticado = usuarioAutenticadoService.obter();

        String requestFingerprint =
                chaveIdempotencia == null
                        ? null
                        : calcularFingerprint(
                        request,
                        assinaturaValidada,
                        fotoValidada
                );
        MovimentacaoResponse repetida =
                buscarOperacaoIdempotente(
                        usuarioAutenticado,
                        chaveIdempotencia,
                        requestFingerprint
                );

        if (repetida != null) {
            return repetida;
        }

        Funcionario funcionario = funcionarioRepository
                .findById(request.getFuncionarioId())
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Funcionário não encontrado"
                        )
                );

        if (request.getTipo() == TipoMovimentacao.RETIRADA
                && !Boolean.TRUE.equals(funcionario.isAtivo())) {
            throw new RegraNegocioException(
                    "Não é possível registrar retirada para um funcionário inativo"
            );
        }

        Contrato contrato = contratoRepository
                .findById(request.getContratoId())
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Contrato não encontrado"
                        )
                );

        if (request.getTipo() == TipoMovimentacao.RETIRADA
                && !Boolean.TRUE.equals(contrato.getAtivo())) {
            throw new RegraNegocioException(
                    "Não é possível registrar retirada em um contrato inativo"
            );
        }

        Material material = buscarMaterialComBloqueio(request.getMaterialId());

        repetida =
                buscarOperacaoIdempotente(
                        usuarioAutenticado,
                        chaveIdempotencia,
                        requestFingerprint
                );

        if (repetida != null) {
            return repetida;
        }

        int estoqueAtual = estoqueAtual(material);

        if (request.getTipo() == TipoMovimentacao.RETIRADA) {
            if (estoqueAtual < request.getQuantidade()) {
                throw new RegraNegocioException(
                        "Quantidade insuficiente em estoque"
                );
            }

            material.setQuantidadeEstoque(
                    estoqueAtual - request.getQuantidade()
            );
        } else if (request.getTipo() == TipoMovimentacao.DEVOLUCAO) {
            long quantidadeAindaRetirada = calcularQuantidadeAindaRetirada(
                    request.getFuncionarioId(),
                    request.getContratoId(),
                    request.getMaterialId()
            );

            if (request.getQuantidade() > quantidadeAindaRetirada) {
                throw new RegraNegocioException(
                        "A devolução não pode ser maior que a quantidade ainda retirada"
                );
            }

            material.setQuantidadeEstoque(
                    somarEstoque(estoqueAtual, request.getQuantidade())
            );
        }

        Movimentacao movimentacao = new Movimentacao();
        movimentacao.setFuncionario(funcionario);
        movimentacao.setContrato(contrato);
        movimentacao.setMaterial(material);
        movimentacao.setQuantidade(request.getQuantidade());
        movimentacao.setTipo(request.getTipo());
        LocalDateTime agora = LocalDateTime.now();
        movimentacao.setDataMovimentacao(agora);
        movimentacao.setDataFinalizacao(agora);
        movimentacao.setObservacao(normalizarObservacao(request.getObservacao()));
        movimentacao.setRegistradoPor(usuarioAutenticado);
        movimentacao.setNotaFiscal(null);
        movimentacao.setIdempotencyKey(chaveIdempotencia);
        movimentacao.setRequestFingerprint(requestFingerprint);

        Movimentacao movimentacaoSalva =
                movimentacaoRepository.saveAndFlush(movimentacao);

        evidenciaService.registrarNaCriacao(movimentacaoSalva, assinaturaValidada, fotoValidada);
        comprovanteService.registrar(movimentacaoSalva);

        return MovimentacaoMapper.paraResponse(movimentacaoSalva);
    }

    @Transactional
    public MovimentacaoResponse estornar(
            Long movimentacaoOrigemId,
            EstornoMovimentacaoRequest request,
            String idempotencyKey
    ) {
        String justificativa = normalizarJustificativa(request.getJustificativa());
        String chaveIdempotencia = normalizarIdempotencyKey(idempotencyKey);
        Usuario usuarioAutenticado = usuarioAutenticadoService.obter();
        String requestFingerprint = chaveIdempotencia == null
                ? null
                : calcularFingerprintEstorno(movimentacaoOrigemId, justificativa);

        MovimentacaoResponse repetida = buscarOperacaoIdempotente(
                usuarioAutenticado,
                chaveIdempotencia,
                requestFingerprint
        );
        if (repetida != null) {
            return repetida;
        }

        Movimentacao origem = movimentacaoRepository
                .findByIdComBloqueio(movimentacaoOrigemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Movimentação não encontrada"
                ));

        repetida = buscarOperacaoIdempotente(
                usuarioAutenticado,
                chaveIdempotencia,
                requestFingerprint
        );
        if (repetida != null) {
            return repetida;
        }

        TipoMovimentacao tipoEstorno = tipoEstornoPara(origem.getTipo());
        if (movimentacaoRepository
                .findByMovimentacaoOrigemId(movimentacaoOrigemId)
                .isPresent()) {
            throw new RegraNegocioException(
                    "A movimentação já foi estornada"
            );
        }

        Material material = buscarMaterialComBloqueio(origem.getMaterial().getId());
        int estoqueAtual = estoqueAtual(material);

        if (tipoEstorno == TipoMovimentacao.ESTORNO_RETIRADA) {
            material.setQuantidadeEstoque(
                    somarEstoque(estoqueAtual, origem.getQuantidade())
            );
        } else {
            if (estoqueAtual < origem.getQuantidade()) {
                throw new RegraNegocioException(
                        "Estoque insuficiente para estornar a devolução"
                );
            }
            material.setQuantidadeEstoque(
                    estoqueAtual - origem.getQuantidade()
            );
        }

        Movimentacao estorno = new Movimentacao();
        estorno.setFuncionario(origem.getFuncionario());
        estorno.setContrato(origem.getContrato());
        estorno.setMaterial(material);
        estorno.setQuantidade(origem.getQuantidade());
        estorno.setTipo(tipoEstorno);
        LocalDateTime agora = LocalDateTime.now();
        estorno.setDataMovimentacao(agora);
        estorno.setDataFinalizacao(agora);
        estorno.setObservacao(justificativa);
        estorno.setRegistradoPor(usuarioAutenticado);
        estorno.setNotaFiscal(null);
        estorno.setMovimentacaoOrigem(origem);
        estorno.setIdempotencyKey(chaveIdempotencia);
        estorno.setRequestFingerprint(requestFingerprint);

        Movimentacao estornoSalvo = movimentacaoRepository.saveAndFlush(estorno);
        comprovanteService.registrar(estornoSalvo);

        return MovimentacaoMapper.paraResponse(estornoSalvo);
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoResponse> listarTodas() {
        return mapearComSituacaoEstorno(movimentacaoRepository.findAll());
    }

    @Transactional(readOnly = true)
    public MovimentacaoResponse listarPorId(Long id) {
        return movimentacaoRepository.findById(id)
                .map(this::mapearComSituacaoEstorno)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Movimentação não encontrada"
                        )
                );
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoResponse> listarPorFuncionario(
            Long funcionarioId
    ) {
        return mapearComSituacaoEstorno(
                movimentacaoRepository.findByFuncionarioId(funcionarioId)
        );
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoResponse> listarPorContrato(
            Long contratoId
    ) {
        return mapearComSituacaoEstorno(
                movimentacaoRepository.findByContratoId(contratoId)
        );
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoResponse> listarPorMaterial(
            Long materialId
    ) {
        return mapearComSituacaoEstorno(
                movimentacaoRepository.findByMaterialId(materialId)
        );
    }

    private Material buscarMaterialComBloqueio(Long materialId) {
        return materialRepository.findByIdComBloqueio(materialId)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Material não encontrado"
                        )
                );
    }



    private void validarQuantidade(Integer quantidade) {
        if (quantidade == null || quantidade <= 0) {
            throw new RegraNegocioException(
                    "A quantidade deve ser maior que zero"
            );
        }

        if (quantidade > LIMITE_QUANTIDADE_OPERACAO) {
            throw new RegraNegocioException(
                    "A quantidade máxima por operação é 10.000"
            );
        }
    }

    private void validarTipoMovimentacaoComum(TipoMovimentacao tipo) {
        if (tipo == null) {
            throw new RegraNegocioException(
                    "O tipo da movimentação é obrigatório"
            );
        }

        if (tipo == TipoMovimentacao.ENTRADA) {
            throw new RegraNegocioException(
                    "Entradas devem ser registradas pela confirmacao de uma nota fiscal"
            );
        }

        if (tipo != TipoMovimentacao.RETIRADA
                && tipo != TipoMovimentacao.DEVOLUCAO) {
            throw new RegraNegocioException(
                    "Tipo de movimentação inválido"
            );
        }
    }

    private int estoqueAtual(Material material) {
        int estoqueAtual = material.getQuantidadeEstoque() == null
                ? 0
                : material.getQuantidadeEstoque();

        if (estoqueAtual < 0) {
            throw new RegraNegocioException(
                    "O estoque do material não pode ser negativo"
            );
        }

        return estoqueAtual;
    }

    private int somarEstoque(int estoqueAtual, int quantidade) {
        try {
            return Math.addExact(estoqueAtual, quantidade);
        } catch (ArithmeticException exception) {
            throw new RegraNegocioException(
                    "A soma do estoque excede o limite suportado pelo sistema"
            );
        }
    }

    private long calcularQuantidadeAindaRetirada(
            Long funcionarioId,
            Long contratoId,
            Long materialId
    ) {
        return movimentacaoRepository
                .findByFuncionarioIdAndContratoIdAndMaterialId(
                        funcionarioId,
                        contratoId,
                        materialId
                )
                .stream()
                .mapToLong(movimentacao -> {
                    if (movimentacao.getTipo() == TipoMovimentacao.RETIRADA) {
                        return movimentacao.getQuantidade();
                    }
                    if (movimentacao.getTipo() == TipoMovimentacao.DEVOLUCAO) {
                        return -movimentacao.getQuantidade();
                    }
                    if (movimentacao.getTipo() == TipoMovimentacao.ESTORNO_RETIRADA) {
                        return -movimentacao.getQuantidade();
                    }
                    if (movimentacao.getTipo() == TipoMovimentacao.ESTORNO_DEVOLUCAO) {
                        return movimentacao.getQuantidade();
                    }
                    return 0;
                })
                .sum();
    }

    private String normalizarObservacao(String observacao) {
        if (observacao == null || observacao.isBlank()) {
            return null;
        }
        return observacao.trim();
    }

    private String normalizarJustificativa(String justificativa) {
        if (justificativa == null || justificativa.isBlank()) {
            throw new RegraNegocioException(
                    "A justificativa do estorno é obrigatória"
            );
        }

        String normalizada = justificativa.trim();
        if (normalizada.length() > 1000) {
            throw new RegraNegocioException(
                    "A justificativa deve possuir no máximo 1.000 caracteres"
            );
        }
        return normalizada;
    }

    private TipoMovimentacao tipoEstornoPara(TipoMovimentacao tipoOrigem) {
        return switch (tipoOrigem) {
            case RETIRADA -> TipoMovimentacao.ESTORNO_RETIRADA;
            case DEVOLUCAO -> TipoMovimentacao.ESTORNO_DEVOLUCAO;
            case ENTRADA -> throw new RegraNegocioException(
                    "Entradas de Nota Fiscal não podem ser estornadas"
            );
            case ESTORNO_RETIRADA, ESTORNO_DEVOLUCAO ->
                    throw new RegraNegocioException(
                            "Uma movimentação de estorno não pode ser estornada"
                    );
        };
    }

    private List<MovimentacaoResponse> mapearComSituacaoEstorno(
            List<Movimentacao> movimentacoes
    ) {
        if (movimentacoes.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> estornoPorOrigem = movimentacaoRepository
                .findByMovimentacaoOrigemIdIn(
                        movimentacoes.stream().map(Movimentacao::getId).toList()
                )
                .stream()
                .collect(Collectors.toMap(
                        movimentacao -> movimentacao.getMovimentacaoOrigem().getId(),
                        Movimentacao::getId
                ));

        return movimentacoes.stream()
                .map(movimentacao -> MovimentacaoMapper.paraResponse(
                        movimentacao,
                        estornoPorOrigem.get(movimentacao.getId())
                ))
                .toList();
    }

    private MovimentacaoResponse mapearComSituacaoEstorno(
            Movimentacao movimentacao
    ) {
        Long estornoId = movimentacaoRepository
                .findByMovimentacaoOrigemId(movimentacao.getId())
                .map(Movimentacao::getId)
                .orElse(null);
        return MovimentacaoMapper.paraResponse(movimentacao, estornoId);
    }


    private MovimentacaoResponse buscarOperacaoIdempotente(
            Usuario usuario,
            String idempotencyKey,
            String requestFingerprint
    ) {
        if (idempotencyKey == null) {
            return null;
        }

        return movimentacaoRepository
                .findByRegistradoPorIdAndIdempotencyKey(
                        usuario.getId(),
                        idempotencyKey
                )
                .map(movimentacaoExistente -> {

                    if (!Objects.equals(
                            movimentacaoExistente.getRequestFingerprint(),
                            requestFingerprint
                    )) {
                        throw new RegraNegocioException(
                                "A chave de idempotência já foi utilizada em outra operação"
                        );
                    }

                    return MovimentacaoMapper
                            .paraResponse(movimentacaoExistente);
                })
                .orElse(null);
    }

    private String normalizarIdempotencyKey(
            String idempotencyKey
    ) {
        if (idempotencyKey == null
                || idempotencyKey.isBlank()) {
            return null;
        }

        String normalizada =
                idempotencyKey.trim();

        if (normalizada.length() > 100) {
            throw new RegraNegocioException(
                    "Idempotency-Key excede o tamanho máximo permitido"
            );
        }

        if (!normalizada.matches(
                "[A-Za-z0-9._:-]+"
        )) {
            throw new RegraNegocioException(
                    "Idempotency-Key inválida"
            );
        }

        return normalizada;
    }

    private String calcularFingerprint(
            MovimentacaoRequest request,
            ImagemEvidenciaValidator.ImagemValidada assinatura,
            ImagemEvidenciaValidator.ImagemValidada foto
    ) {
        MessageDigest digest = novoSha256();

        atualizarFingerprint(
                digest,
                request.getFuncionarioId()
        );

        atualizarFingerprint(
                digest,
                request.getContratoId()
        );

        atualizarFingerprint(
                digest,
                request.getMaterialId()
        );

        atualizarFingerprint(
                digest,
                request.getQuantidade()
        );

        atualizarFingerprint(
                digest,
                request.getTipo()
        );

        atualizarFingerprint(
                digest,
                normalizarObservacao(
                        request.getObservacao()
                )
        );

        atualizarFingerprint(
                digest,
                assinatura == null
                        ? null
                        : assinatura.conteudo()
        );

        atualizarFingerprint(
                digest,
                foto == null
                        ? null
                        : foto.conteudo()
        );

        return HexFormat.of()
                .formatHex(digest.digest());
    }

    private String calcularFingerprintEstorno(
            Long movimentacaoOrigemId,
            String justificativa
    ) {
        MessageDigest digest = novoSha256();
        atualizarFingerprint(digest, "ESTORNO");
        atualizarFingerprint(digest, movimentacaoOrigemId);
        atualizarFingerprint(digest, justificativa);
        return HexFormat.of().formatHex(digest.digest());
    }

    private void atualizarFingerprint(
            MessageDigest digest,
            Object valor
    ) {
        if (valor == null) {
            digest.update(
                    ByteBuffer
                            .allocate(Integer.BYTES)
                            .putInt(-1)
                            .array()
            );
            return;
        }

        byte[] bytes =
                String.valueOf(valor)
                        .getBytes(StandardCharsets.UTF_8);

        atualizarFingerprint(
                digest,
                bytes
        );
    }

    private void atualizarFingerprint(
            MessageDigest digest,
            byte[] bytes
    ) {
        if (bytes == null) {
            digest.update(
                    ByteBuffer
                            .allocate(Integer.BYTES)
                            .putInt(-1)
                            .array()
            );
            return;
        }

        digest.update(
                ByteBuffer
                        .allocate(Integer.BYTES)
                        .putInt(bytes.length)
                        .array()
        );

        digest.update(bytes);
    }

    private MessageDigest novoSha256() {
        try {
            return MessageDigest.getInstance(
                    "SHA-256"
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 não está disponível",
                    exception
            );
        }
    }

}
