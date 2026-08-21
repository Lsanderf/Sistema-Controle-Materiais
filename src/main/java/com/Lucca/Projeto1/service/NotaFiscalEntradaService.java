package com.Lucca.Projeto1.service;

import com.Lucca.Projeto1.dto.movimentacao.MovimentacaoResponse;
import com.Lucca.Projeto1.dto.notafiscal.ItemNotaFiscalRequest;
import com.Lucca.Projeto1.dto.notafiscal.NotaFiscalRequest;
import com.Lucca.Projeto1.dto.notafiscal.NotaFiscalResponse;
import com.Lucca.Projeto1.exception.RecursoNaoEncontradoException;
import com.Lucca.Projeto1.exception.RegraNegocioException;
import com.Lucca.Projeto1.mapper.MovimentacaoMapper;
import com.Lucca.Projeto1.mapper.NotaFiscalEntradaMapper;
import com.Lucca.Projeto1.model.ItemNotaFiscal;
import com.Lucca.Projeto1.model.Material;
import com.Lucca.Projeto1.model.Movimentacao;
import com.Lucca.Projeto1.model.NotaFiscalEntrada;
import com.Lucca.Projeto1.model.StatusNotaFiscal;
import com.Lucca.Projeto1.model.TipoMovimentacao;
import com.Lucca.Projeto1.model.Usuario;
import com.Lucca.Projeto1.repository.MaterialRepository;
import com.Lucca.Projeto1.repository.MovimentacaoRepository;
import com.Lucca.Projeto1.repository.NotaFiscalEntradaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NotaFiscalEntradaService {

    private static final int LIMITE_QUANTIDADE_ITEM = 10000;

    private final NotaFiscalEntradaRepository notaFiscalRepository;
    private final MaterialRepository materialRepository;
    private final MovimentacaoRepository movimentacaoRepository;
    private final UsuarioAutenticadoService usuarioAutenticadoService;

    public NotaFiscalEntradaService(
            NotaFiscalEntradaRepository notaFiscalRepository,
            MaterialRepository materialRepository,
            MovimentacaoRepository movimentacaoRepository,
            UsuarioAutenticadoService usuarioAutenticadoService
    ) {
        this.notaFiscalRepository = notaFiscalRepository;
        this.materialRepository = materialRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.usuarioAutenticadoService = usuarioAutenticadoService;
    }

    @Transactional
    public NotaFiscalResponse criar(NotaFiscalRequest request) {
        String chaveAcesso = normalizarChaveAcesso(request.getChaveAcesso());
        validarChaveDuplicada(chaveAcesso, null);

        NotaFiscalEntrada notaFiscal = new NotaFiscalEntrada();
        notaFiscal.setStatus(StatusNotaFiscal.RASCUNHO);
        notaFiscal.setCadastradaPor(usuarioAutenticadoService.obter());
        notaFiscal.setDataCadastro(LocalDateTime.now());

        preencherDados(notaFiscal, request, chaveAcesso);
        substituirItens(notaFiscal, request.getItens());

        NotaFiscalEntrada notaSalva =
                notaFiscalRepository.saveAndFlush(notaFiscal);

        return NotaFiscalEntradaMapper.paraResponse(
                notaSalva,
                Collections.emptyList()
        );
    }

    @Transactional(readOnly = true)
    public List<NotaFiscalResponse> listarTodas() {
        List<NotaFiscalEntrada> notas =
                notaFiscalRepository.findAllDetalhadas();

        if (notas.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, List<MovimentacaoResponse>> movimentacoesPorNota =
                buscarMovimentacoesPorNota(
                        notas.stream().map(NotaFiscalEntrada::getId).toList()
                );

        return notas.stream()
                .map(nota -> NotaFiscalEntradaMapper.paraResponse(
                        nota,
                        movimentacoesPorNota.getOrDefault(
                                nota.getId(),
                                Collections.emptyList()
                        )
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public NotaFiscalResponse buscarPorId(Long id) {
        NotaFiscalEntrada notaFiscal = notaFiscalRepository
                .findDetalhadaById(id)
                .orElseThrow(() -> novaNotaNaoEncontrada(id));

        List<MovimentacaoResponse> movimentacoes = movimentacaoRepository
                .findByNotaFiscalIdOrderByIdAsc(id)
                .stream()
                .map(MovimentacaoMapper::paraResponse)
                .toList();

        return NotaFiscalEntradaMapper.paraResponse(
                notaFiscal,
                movimentacoes
        );
    }

    @Transactional
    public NotaFiscalResponse atualizar(
            Long id,
            NotaFiscalRequest request
    ) {
        NotaFiscalEntrada notaFiscal = buscarComBloqueio(id);
        validarRascunho(notaFiscal, "alterada");

        String chaveAcesso = normalizarChaveAcesso(request.getChaveAcesso());
        validarChaveDuplicada(chaveAcesso, id);

        preencherDados(notaFiscal, request, chaveAcesso);
        substituirItens(notaFiscal, request.getItens());
        notaFiscalRepository.flush();

        return NotaFiscalEntradaMapper.paraResponse(
                notaFiscal,
                Collections.emptyList()
        );
    }

    @Transactional
    public NotaFiscalResponse confirmarEntrada(Long id) {
        NotaFiscalEntrada notaFiscal = buscarComBloqueio(id);
        validarRascunho(notaFiscal, "confirmada novamente");

        if (notaFiscal.getItens().isEmpty()) {
            throw new RegraNegocioException(
                    "A nota fiscal deve possuir pelo menos um item para ser confirmada"
            );
        }

        validarItensPersistidos(notaFiscal.getItens());
        Map<Long, Material> materiaisBloqueados =
                buscarMateriaisComBloqueio(notaFiscal.getItens());
        Usuario usuarioAutenticado = usuarioAutenticadoService.obter();
        LocalDateTime agora = LocalDateTime.now();

        List<Movimentacao> movimentacoes = notaFiscal.getItens()
                .stream()
                .map(item -> processarItem(
                        notaFiscal,
                        item,
                        materiaisBloqueados,
                        usuarioAutenticado,
                        agora
                ))
                .toList();

        notaFiscal.setStatus(StatusNotaFiscal.CONFIRMADA);
        notaFiscal.setDataEntrada(agora);
        movimentacaoRepository.saveAllAndFlush(movimentacoes);
        notaFiscalRepository.flush();

        List<MovimentacaoResponse> respostasMovimentacoes = movimentacoes
                .stream()
                .map(MovimentacaoMapper::paraResponse)
                .toList();

        return NotaFiscalEntradaMapper.paraResponse(
                notaFiscal,
                respostasMovimentacoes
        );
    }

    private void preencherDados(
            NotaFiscalEntrada notaFiscal,
            NotaFiscalRequest request,
            String chaveAcesso
    ) {
        notaFiscal.setNumero(request.getNumero().trim());
        notaFiscal.setSerie(request.getSerie().trim());
        notaFiscal.setChaveAcesso(chaveAcesso);
        notaFiscal.setFornecedor(request.getFornecedor().trim());
        notaFiscal.setCnpjFornecedor(
                normalizarCnpj(request.getCnpjFornecedor())
        );
        notaFiscal.setDataEmissao(request.getDataEmissao());
    }

    private void substituirItens(
            NotaFiscalEntrada notaFiscal,
            List<ItemNotaFiscalRequest> itensRequest
    ) {
        if (itensRequest == null) {
            throw new RegraNegocioException(
                    "A lista de itens é obrigatória, mas pode estar vazia"
            );
        }

        itensRequest.forEach(this::validarItemRequest);
        Map<Long, Material> materiais = buscarMateriais(itensRequest);

        notaFiscal.removerTodosOsItens();

        for (ItemNotaFiscalRequest itemRequest : itensRequest) {
            ItemNotaFiscal item = new ItemNotaFiscal();
            item.setMaterial(materiais.get(itemRequest.getMaterialId()));
            item.setQuantidade(itemRequest.getQuantidade());
            item.setValorUnitario(
                    itemRequest.getValorUnitario().setScale(
                            2,
                            RoundingMode.UNNECESSARY
                    )
            );
            notaFiscal.adicionarItem(item);
        }
    }

    private Map<Long, Material> buscarMateriais(
            List<ItemNotaFiscalRequest> itens
    ) {
        Set<Long> ids = itens.stream()
                .map(ItemNotaFiscalRequest::getMaterialId)
                .collect(Collectors.toSet());

        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Material> materiais = materialRepository
                .findAllById(ids)
                .stream()
                .collect(Collectors.toMap(Material::getId, Function.identity()));

        validarTodosMateriaisEncontrados(ids, materiais);
        return materiais;
    }

    private Map<Long, Material> buscarMateriaisComBloqueio(
            List<ItemNotaFiscal> itens
    ) {
        Set<Long> ids = itens.stream()
                .map(item -> item.getMaterial().getId())
                .collect(Collectors.toSet());

        Map<Long, Material> materiais = materialRepository
                .findAllByIdComBloqueio(ids)
                .stream()
                .collect(Collectors.toMap(
                        Material::getId,
                        Function.identity(),
                        (primeiro, segundo) -> primeiro,
                        LinkedHashMap::new
                ));

        validarTodosMateriaisEncontrados(ids, materiais);
        return materiais;
    }

    private void validarTodosMateriaisEncontrados(
            Set<Long> ids,
            Map<Long, Material> materiais
    ) {
        ids.stream()
                .filter(id -> !materiais.containsKey(id))
                .findFirst()
                .ifPresent(id -> {
                    throw new RecursoNaoEncontradoException(
                            "Material com ID " + id + " não encontrado"
                    );
                });
    }

    private Movimentacao processarItem(
            NotaFiscalEntrada notaFiscal,
            ItemNotaFiscal item,
            Map<Long, Material> materiais,
            Usuario usuario,
            LocalDateTime dataMovimentacao
    ) {
        Material material = materiais.get(item.getMaterial().getId());
        int estoqueAtual = material.getQuantidadeEstoque() == null
                ? 0
                : material.getQuantidadeEstoque();

        if (estoqueAtual < 0) {
            throw new RegraNegocioException(
                    "O estoque do material não pode ser negativo"
            );
        }

        try {
            material.setQuantidadeEstoque(
                    Math.addExact(estoqueAtual, item.getQuantidade())
            );
        } catch (ArithmeticException exception) {
            throw new RegraNegocioException(
                    "A soma do estoque excede o limite suportado pelo sistema"
            );
        }

        Movimentacao movimentacao = new Movimentacao();
        movimentacao.setMaterial(material);
        movimentacao.setQuantidade(item.getQuantidade());
        movimentacao.setTipo(TipoMovimentacao.ENTRADA);
        movimentacao.setDataMovimentacao(dataMovimentacao);
        movimentacao.setFuncionario(null);
        movimentacao.setContrato(null);
        movimentacao.setRegistradoPor(usuario);
        movimentacao.setNotaFiscal(notaFiscal);
        return movimentacao;
    }

    private void validarItemRequest(ItemNotaFiscalRequest item) {
        if (item == null) {
            throw new RegraNegocioException(
                    "A nota fiscal não pode possuir item nulo"
            );
        }
        validarQuantidade(item.getQuantidade());
        validarValorUnitario(item.getValorUnitario());
        if (item.getMaterialId() == null || item.getMaterialId() <= 0) {
            throw new RegraNegocioException(
                    "O material do item é obrigatório"
            );
        }
    }

    private void validarItensPersistidos(Collection<ItemNotaFiscal> itens) {
        for (ItemNotaFiscal item : itens) {
            if (item.getMaterial() == null) {
                throw new RegraNegocioException(
                        "Todos os itens devem possuir um material"
                );
            }
            validarQuantidade(item.getQuantidade());
            validarValorUnitario(item.getValorUnitario());
        }
    }

    private void validarQuantidade(Integer quantidade) {
        if (quantidade == null || quantidade <= 0) {
            throw new RegraNegocioException(
                    "A quantidade do item deve ser maior que zero"
            );
        }
        if (quantidade > LIMITE_QUANTIDADE_ITEM) {
            throw new RegraNegocioException(
                    "A quantidade máxima por item é 10.000"
            );
        }
    }

    private void validarValorUnitario(BigDecimal valorUnitario) {
        if (valorUnitario == null || valorUnitario.signum() < 0) {
            throw new RegraNegocioException(
                    "O valor unitário do item não pode ser negativo"
            );
        }
        if (valorUnitario.scale() > 2 || valorUnitario.precision() > 19) {
            throw new RegraNegocioException(
                    "O valor unitário deve possuir no máximo 17 inteiros e 2 decimais"
            );
        }
    }

    private void validarRascunho(
            NotaFiscalEntrada notaFiscal,
            String operacao
    ) {
        if (notaFiscal.getStatus() != StatusNotaFiscal.RASCUNHO) {
            throw new RegraNegocioException(
                    "Uma nota fiscal confirmada não pode ser " + operacao
            );
        }
    }

    private void validarChaveDuplicada(String chaveAcesso, Long notaId) {
        boolean duplicada = notaId == null
                ? notaFiscalRepository.existsByChaveAcesso(chaveAcesso)
                : notaFiscalRepository.existsByChaveAcessoAndIdNot(
                        chaveAcesso,
                        notaId
                );

        if (duplicada) {
            throw new RegraNegocioException(
                    "Já existe uma nota fiscal com essa chave de acesso"
            );
        }
    }

    private String normalizarChaveAcesso(String chaveAcesso) {
        if (chaveAcesso == null
                || !chaveAcesso.matches("[0-9.\\-/\\s]+")) {
            throw new RegraNegocioException(
                    "A chave de acesso deve conter apenas números e formatação"
            );
        }

        String normalizada = chaveAcesso.replaceAll("\\D", "");
        if (normalizada.length() != 44) {
            throw new RegraNegocioException(
                    "A chave de acesso deve conter exatamente 44 dígitos"
            );
        }
        return normalizada;
    }

    private String normalizarCnpj(String cnpj) {
        if (cnpj == null) {
            throw new RegraNegocioException(
                    "O CNPJ do fornecedor é obrigatório"
            );
        }

        String normalizado = cnpj.replaceAll("\\D", "");
        if (normalizado.length() != 14) {
            throw new RegraNegocioException(
                    "O CNPJ do fornecedor deve conter 14 dígitos"
            );
        }
        return normalizado;
    }

    private NotaFiscalEntrada buscarComBloqueio(Long id) {
        return notaFiscalRepository.findByIdComBloqueio(id)
                .orElseThrow(() -> novaNotaNaoEncontrada(id));
    }

    private RecursoNaoEncontradoException novaNotaNaoEncontrada(Long id) {
        return new RecursoNaoEncontradoException(
                "Nota fiscal com ID " + id + " não encontrada"
        );
    }

    private Map<Long, List<MovimentacaoResponse>> buscarMovimentacoesPorNota(
            List<Long> notasIds
    ) {
        return movimentacaoRepository
                .findByNotaFiscalIdInOrderByNotaFiscalIdAscIdAsc(notasIds)
                .stream()
                .collect(Collectors.groupingBy(
                        movimentacao -> movimentacao.getNotaFiscal().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(
                                MovimentacaoMapper::paraResponse,
                                Collectors.toList()
                        )
                ));
    }
}
