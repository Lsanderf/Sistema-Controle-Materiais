package com.Lucca.Projeto1.service;

import com.Lucca.Projeto1.dto.solicitacaoretirada.SolicitacaoRetiradaRequest;
import com.Lucca.Projeto1.dto.solicitacaoretirada.SolicitacaoRetiradaResponse;
import com.Lucca.Projeto1.exception.EstoqueInsuficienteException;
import com.Lucca.Projeto1.exception.RecursoNaoEncontradoException;
import com.Lucca.Projeto1.exception.RegraNegocioException;
import com.Lucca.Projeto1.model.Contrato;
import com.Lucca.Projeto1.model.Material;
import com.Lucca.Projeto1.model.Role;
import com.Lucca.Projeto1.model.SolicitacaoRetirada;
import com.Lucca.Projeto1.model.SolicitacaoRetiradaItem;
import com.Lucca.Projeto1.model.StatusSolicitacaoRetirada;
import com.Lucca.Projeto1.model.Usuario;
import com.Lucca.Projeto1.repository.ContratoRepository;
import com.Lucca.Projeto1.repository.MaterialRepository;
import com.Lucca.Projeto1.repository.MovimentacaoRepository;
import com.Lucca.Projeto1.repository.SolicitacaoRetiradaRepository;
import com.Lucca.Projeto1.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SolicitacaoRetiradaService {

    private final SolicitacaoRetiradaRepository solicitacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ContratoRepository contratoRepository;
    private final MaterialRepository materialRepository;
    private final UsuarioAutenticadoService usuarioAutenticadoService;
    private final MovimentacaoRepository movimentacaoRepository;
    private final ComprovanteMovimentacaoService comprovanteService;
    private final AssinaturaRetiradaService assinaturaService;
    private final ImagemEvidenciaValidator imagemValidator;

    public SolicitacaoRetiradaService(
            SolicitacaoRetiradaRepository solicitacaoRepository,
            UsuarioRepository usuarioRepository,
            ContratoRepository contratoRepository,
            MaterialRepository materialRepository,
            UsuarioAutenticadoService usuarioAutenticadoService,
            MovimentacaoRepository movimentacaoRepository,
            ComprovanteMovimentacaoService comprovanteService,
            AssinaturaRetiradaService assinaturaService,
            ImagemEvidenciaValidator imagemValidator
    ) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.contratoRepository = contratoRepository;
        this.materialRepository = materialRepository;
        this.usuarioAutenticadoService = usuarioAutenticadoService;
        this.movimentacaoRepository = movimentacaoRepository;
        this.comprovanteService = comprovanteService;
        this.assinaturaService = assinaturaService;
        this.imagemValidator = imagemValidator;
    }

    @Transactional
    public SolicitacaoRetiradaResponse confirmar(Long solicitacaoId, MultipartFile arquivoAssinatura) {
        Usuario encarregadoAutenticado = usuarioAutenticadoService.obter();
        if (encarregadoAutenticado.getRole() != Role.ENCARREGADO) {
            throw new org.springframework.security.access.AccessDeniedException("Apenas encarregado pode confirmar retirada");
        }
        SolicitacaoRetirada solicitacao = solicitacaoRepository.findByIdComBloqueio(solicitacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("SolicitaÃ§Ã£o de retirada nÃ£o encontrada"));
        if (!solicitacao.getEncarregadoAssinante().getId().equals(encarregadoAutenticado.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("SolicitaÃ§Ã£o destinada a outro encarregado");
        }
        if (solicitacao.getStatus() == StatusSolicitacaoRetirada.CONFIRMADA) {
            imagemValidator.validar(arquivoAssinatura, true);
            return paraResponse(solicitacao);
        }
        if (solicitacao.getStatus() != StatusSolicitacaoRetirada.AGUARDANDO_ASSINATURA) {
            throw new RegraNegocioException("A solicitaÃ§Ã£o nÃ£o pode ser confirmada no estado atual");
        }
        if (solicitacao.getContrato() != null && !Boolean.TRUE.equals(solicitacao.getContrato().getAtivo())) {
            throw new RegraNegocioException("NÃ£o Ã© possÃ­vel confirmar retirada para contrato inativo");
        }

        var assinatura = imagemValidator.validar(arquivoAssinatura, true);
        List<Long> idsMateriais = solicitacao.getItens().stream().map(item -> item.getMaterial().getId()).sorted().toList();
        List<Material> materiaisBloqueados = materialRepository.findAllByIdComBloqueio(idsMateriais);
        Map<Long, Material> materiaisPorId = materiaisBloqueados.stream().collect(Collectors.toMap(Material::getId, material -> material));
        List<EstoqueInsuficienteException.Item> faltantes = solicitacao.getItens().stream()
                .map(item -> falta(materiaisPorId.get(item.getMaterial().getId()), item.getQuantidade()))
                .filter(Objects::nonNull).toList();
        if (!faltantes.isEmpty()) throw new EstoqueInsuficienteException(faltantes);

        LocalDateTime agora = LocalDateTime.now();
        List<com.Lucca.Projeto1.model.Movimentacao> movimentacoes = solicitacao.getItens().stream().map(item -> {
            Material material = materiaisPorId.get(item.getMaterial().getId());
            material.setQuantidadeEstoque(material.getQuantidadeEstoque() - item.getQuantidade());
            com.Lucca.Projeto1.model.Movimentacao movimentacao = new com.Lucca.Projeto1.model.Movimentacao();
            movimentacao.setContrato(solicitacao.getContrato());
            movimentacao.setMaterial(material);
            movimentacao.setQuantidade(item.getQuantidade());
            movimentacao.setTipo(com.Lucca.Projeto1.model.TipoMovimentacao.RETIRADA);
            movimentacao.setDataMovimentacao(agora);
            movimentacao.setDataFinalizacao(agora);
            movimentacao.setObservacao(solicitacao.getObservacao());
            movimentacao.setRegistradoPor(solicitacao.getOperadorResponsavel());
            movimentacao.setEncarregadoAssinante(encarregadoAutenticado);
            movimentacao.setSolicitacaoRetirada(solicitacao);
            return movimentacao;
        }).toList();
        List<com.Lucca.Projeto1.model.Movimentacao> salvas = movimentacaoRepository.saveAllAndFlush(movimentacoes);
        assinaturaService.registrar(solicitacao, encarregadoAutenticado, assinatura);
        comprovanteService.registrarTodos(salvas);
        solicitacao.setStatus(StatusSolicitacaoRetirada.CONFIRMADA);
        solicitacao.setConfirmadaEm(agora);
        return paraResponse(solicitacao);
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoRetiradaResponse> listar() {
        Usuario usuario = usuarioAutenticadoService.obter();
        return solicitacaoRepository.findAll().stream()
                .filter(solicitacao -> podeLer(solicitacao, usuario))
                .map(this::paraResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SolicitacaoRetiradaResponse buscarPorId(Long id) {
        Usuario usuario = usuarioAutenticadoService.obter();
        SolicitacaoRetirada solicitacao = buscar(id);
        if (!podeLer(solicitacao, usuario)) {
            throw new org.springframework.security.access.AccessDeniedException("VocÃª nÃ£o possui acesso a esta solicitaÃ§Ã£o");
        }
        return paraResponse(solicitacao);
    }

    @Transactional
    public SolicitacaoRetiradaResponse cancelar(Long id) {
        Usuario usuario = usuarioAutenticadoService.obter();
        SolicitacaoRetirada solicitacao = solicitacaoRepository.findByIdComBloqueio(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("SolicitaÃ§Ã£o de retirada nÃ£o encontrada"));
        boolean criador = usuario.getRole() == Role.OPERADOR
                && solicitacao.getOperadorResponsavel().getId().equals(usuario.getId());
        if (usuario.getRole() != Role.ADMIN && !criador) {
            throw new org.springframework.security.access.AccessDeniedException("Apenas o operador responsÃ¡vel pode cancelar a solicitaÃ§Ã£o");
        }
        if (solicitacao.getStatus() != StatusSolicitacaoRetirada.AGUARDANDO_ASSINATURA) {
            throw new RegraNegocioException("A solicitaÃ§Ã£o nÃ£o pode ser cancelada no estado atual");
        }
        solicitacao.setStatus(StatusSolicitacaoRetirada.CANCELADA);
        solicitacao.setCanceladaEm(LocalDateTime.now());
        return paraResponse(solicitacao);
    }

    private SolicitacaoRetirada buscar(Long id) {
        return solicitacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("SolicitaÃ§Ã£o de retirada nÃ£o encontrada"));
    }

    private boolean podeLer(SolicitacaoRetirada solicitacao, Usuario usuario) {
        return usuario.getRole() == Role.ADMIN
                || (usuario.getRole() == Role.OPERADOR && solicitacao.getOperadorResponsavel().getId().equals(usuario.getId()))
                || (usuario.getRole() == Role.ENCARREGADO && solicitacao.getEncarregadoAssinante().getId().equals(usuario.getId()));
    }

    @Transactional
    public SolicitacaoRetiradaResponse criar(SolicitacaoRetiradaRequest request, String idempotencyKey) {
        Usuario operador = usuarioAutenticadoService.obter();
        exigirOperadorOuAdmin(operador);
        String chave = normalizarIdempotencyKey(idempotencyKey);
        String fingerprint = chave == null ? null : fingerprint(request);

        if (chave != null) {
            SolicitacaoRetirada repetida = solicitacaoRepository
                    .findByOperadorResponsavelIdAndIdempotencyKey(operador.getId(), chave)
                    .orElse(null);
            if (repetida != null) {
                if (!Objects.equals(repetida.getRequestFingerprint(), fingerprint)) {
                    throw new RegraNegocioException("A chave de idempotÃªncia jÃ¡ foi utilizada em outra solicitaÃ§Ã£o");
                }
                return paraResponse(repetida);
            }
        }

        Usuario encarregado = usuarioRepository.findById(request.encarregadoAssinanteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Encarregado nÃ£o encontrado"));
        if (encarregado.getRole() != Role.ENCARREGADO || !encarregado.isAtivo()) {
            throw new RegraNegocioException("O assinante deve ser um encarregado ativo");
        }

        Contrato contrato = buscarContratoAtivo(request.contratoId());
        Map<Long, Integer> quantidades = quantidadesPorMaterial(request.itens());
        List<Long> idsMateriais = quantidades.keySet().stream().sorted().toList();
        List<Material> materiaisBloqueados = materialRepository.findAllByIdComBloqueio(idsMateriais);
        if (materiaisBloqueados.size() != idsMateriais.size()) {
            throw new RecursoNaoEncontradoException("Material nÃ£o encontrado");
        }
        Map<Long, Material> materiaisPorId = materiaisBloqueados.stream()
                .collect(Collectors.toMap(Material::getId, material -> material));
        List<EstoqueInsuficienteException.Item> faltantes = idsMateriais.stream()
                .map(id -> falta(materiaisPorId.get(id), quantidades.get(id)))
                .filter(Objects::nonNull)
                .toList();
        if (!faltantes.isEmpty()) {
            throw new EstoqueInsuficienteException(faltantes);
        }

        SolicitacaoRetirada solicitacao = new SolicitacaoRetirada();
        solicitacao.setOperadorResponsavel(operador);
        solicitacao.setEncarregadoAssinante(encarregado);
        solicitacao.setContrato(contrato);
        solicitacao.setStatus(StatusSolicitacaoRetirada.AGUARDANDO_ASSINATURA);
        solicitacao.setObservacao(normalizarObservacao(request.observacao()));
        solicitacao.setCriadaEm(LocalDateTime.now());
        solicitacao.setIdempotencyKey(chave);
        solicitacao.setRequestFingerprint(fingerprint);
        idsMateriais.forEach(id -> {
            SolicitacaoRetiradaItem item = new SolicitacaoRetiradaItem();
            item.setMaterial(materiaisPorId.get(id));
            item.setQuantidade(quantidades.get(id));
            solicitacao.adicionarItem(item);
        });
        return paraResponse(solicitacaoRepository.saveAndFlush(solicitacao));
    }

    private Contrato buscarContratoAtivo(Long contratoId) {
        if (contratoId == null) return null;
        Contrato contrato = contratoRepository.findById(contratoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Contrato nÃ£o encontrado"));
        if (!Boolean.TRUE.equals(contrato.getAtivo())) {
            throw new RegraNegocioException("NÃ£o Ã© possÃ­vel solicitar retirada para contrato inativo");
        }
        return contrato;
    }

    private Map<Long, Integer> quantidadesPorMaterial(List<SolicitacaoRetiradaRequest.Item> itens) {
        Map<Long, Integer> quantidades = new HashMap<>();
        for (SolicitacaoRetiradaRequest.Item item : itens) {
            if (quantidades.putIfAbsent(item.materialId(), item.quantidade()) != null) {
                throw new RegraNegocioException("Um material nÃ£o pode ser informado mais de uma vez");
            }
        }
        return quantidades;
    }

    private EstoqueInsuficienteException.Item falta(Material material, Integer solicitado) {
        int disponivel = material.getQuantidadeEstoque() == null ? 0 : material.getQuantidadeEstoque();
        if (disponivel >= solicitado) return null;
        return new EstoqueInsuficienteException.Item(material.getId(), material.getNome(), disponivel, solicitado, solicitado - disponivel);
    }

    private void exigirOperadorOuAdmin(Usuario usuario) {
        if (usuario.getRole() != Role.OPERADOR && usuario.getRole() != Role.ADMIN) {
            throw new org.springframework.security.access.AccessDeniedException("Perfil sem permissÃ£o para preparar retirada");
        }
    }

    private String normalizarIdempotencyKey(String key) {
        if (key == null || key.isBlank()) return null;
        String normalizada = key.trim();
        if (normalizada.length() > 100) throw new RegraNegocioException("Idempotency-Key deve possuir no mÃ¡ximo 100 caracteres");
        return normalizada;
    }

    private String fingerprint(SolicitacaoRetiradaRequest request) {
        String bruto = request.encarregadoAssinanteId() + "|" + request.contratoId() + "|" + normalizarObservacao(request.observacao()) + "|" +
                request.itens().stream().sorted(Comparator.comparing(SolicitacaoRetiradaRequest.Item::materialId))
                        .map(item -> item.materialId() + ":" + item.quantidade()).collect(Collectors.joining(","));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bruto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponÃ­vel", exception);
        }
    }

    private String normalizarObservacao(String observacao) {
        if (observacao == null || observacao.isBlank()) return null;
        return observacao.trim();
    }

    private SolicitacaoRetiradaResponse paraResponse(SolicitacaoRetirada solicitacao) {
        Usuario operador = solicitacao.getOperadorResponsavel();
        Usuario encarregado = solicitacao.getEncarregadoAssinante();
        Contrato contrato = solicitacao.getContrato();
        return new SolicitacaoRetiradaResponse(
                solicitacao.getId(),
                new SolicitacaoRetiradaResponse.UsuarioResumo(operador.getId(), operador.getNome()),
                new SolicitacaoRetiradaResponse.UsuarioResumo(encarregado.getId(), encarregado.getNome()),
                contrato == null ? null : new SolicitacaoRetiradaResponse.ContratoResumo(contrato.getId(), contrato.getNome()),
                solicitacao.getStatus(), solicitacao.getObservacao(), solicitacao.getCriadaEm(), solicitacao.getConfirmadaEm(),
                solicitacao.getItens().stream().map(item -> new SolicitacaoRetiradaResponse.Item(
                        item.getId(), item.getMaterial().getId(), item.getMaterial().getNome(), item.getQuantidade()
                )).toList()
        );
    }
}
