package com.Lucca.Projeto1.service;

import com.Lucca.Projeto1.dto.requisicao.RequisicaoFaltaEstoqueRequest;
import com.Lucca.Projeto1.dto.requisicao.RequisicaoEdicaoRequest;
import com.Lucca.Projeto1.dto.requisicao.RequisicaoItemRequest;
import com.Lucca.Projeto1.dto.requisicao.RequisicaoRequest;
import com.Lucca.Projeto1.dto.requisicao.RequisicaoResponse;
import com.Lucca.Projeto1.exception.RecursoNaoEncontradoException;
import com.Lucca.Projeto1.exception.RegraNegocioException;
import com.Lucca.Projeto1.model.Contrato;
import com.Lucca.Projeto1.model.Material;
import com.Lucca.Projeto1.model.OrigemRequisicao;
import com.Lucca.Projeto1.model.Requisicao;
import com.Lucca.Projeto1.model.RequisicaoItem;
import com.Lucca.Projeto1.model.Role;
import com.Lucca.Projeto1.model.StatusRequisicao;
import com.Lucca.Projeto1.model.TipoMovimentacao;
import com.Lucca.Projeto1.model.Usuario;
import com.Lucca.Projeto1.repository.ContratoRepository;
import com.Lucca.Projeto1.repository.MaterialRepository;
import com.Lucca.Projeto1.repository.RequisicaoRepository;
import com.Lucca.Projeto1.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RequisicaoService {
    private final RequisicaoRepository requisicaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ContratoRepository contratoRepository;
    private final UsuarioAutenticadoService usuarioAutenticadoService;
    private final MaterialRepository materialRepository;

    public RequisicaoService(RequisicaoRepository requisicaoRepository, UsuarioRepository usuarioRepository,
            ContratoRepository contratoRepository, UsuarioAutenticadoService usuarioAutenticadoService,
            MaterialRepository materialRepository) {
        this.requisicaoRepository = requisicaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.contratoRepository = contratoRepository;
        this.usuarioAutenticadoService = usuarioAutenticadoService;
        this.materialRepository = materialRepository;
    }

    /** Preserves the original manager-to-encarregado requisition workflow. */
    @Transactional
    public RequisicaoResponse criar(RequisicaoRequest request) {
        Usuario gerente = usuarioAutenticadoService.obter();
        exigirRole(gerente, Role.GERENTE, "Only managers can create manual requisitions");
        validarTipo(request.tipo());
        Usuario encarregado = buscarUsuarioAtivo(request.encarregadoDestinatarioId(), Role.ENCARREGADO,
                "Encarregado not found");
        Contrato contrato = buscarContratoAtivo(request.contratoId());

        Requisicao requisicao = new Requisicao();
        requisicao.setGerenteSolicitante(gerente);
        requisicao.setEncarregadoDestinatario(encarregado);
        requisicao.setContrato(contrato);
        requisicao.setTipo(request.tipo());
        requisicao.setOrigem(OrigemRequisicao.MANUAL);
        requisicao.setObservacao(normalizarObservacao(request.observacao()));
        requisicao.setStatus(StatusRequisicao.PENDENTE);
        requisicao.setCriadaEm(LocalDateTime.now());
        for (RequisicaoItemRequest itemRequest : request.itens()) {
            RequisicaoItem item = new RequisicaoItem();
            item.setDescricao(itemRequest.descricao().trim());
            item.setQuantidade(itemRequest.quantidade());
            requisicao.adicionarItem(item);
        }
        return paraResponse(requisicaoRepository.save(requisicao), gerente);
    }

    /** Registers stock shortage for the manager responsible for purchasing. */
    @Transactional
    public RequisicaoResponse criarPorFaltaEstoque(RequisicaoFaltaEstoqueRequest request) {
        Usuario operador = usuarioAutenticadoService.obter();
        if (operador.getRole() != Role.OPERADOR && operador.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only operators can register stock shortages");
        }
        Usuario gerente = buscarUsuarioAtivo(request.gerenteDestinatarioId(), Role.GERENTE, "Manager not found");
        Usuario encarregado = buscarUsuarioAtivo(request.encarregadoNecessidadeId(), Role.ENCARREGADO,
                "Encarregado not found");
        Contrato contrato = buscarContratoAtivo(request.contratoId());

        Map<Long, Integer> solicitados = new HashMap<>();
        for (RequisicaoFaltaEstoqueRequest.Item item : request.itens()) {
            if (solicitados.putIfAbsent(item.materialId(), item.quantidadeSolicitada()) != null) {
                throw new RegraNegocioException("A material can only be submitted once");
            }
        }
        List<Long> ids = solicitados.keySet().stream().sorted().toList();
        List<Material> materiais = materialRepository.findAllByIdComBloqueio(ids);
        if (materiais.size() != ids.size()) throw new RecursoNaoEncontradoException("Material not found");
        Map<Long, Material> porId = materiais.stream().collect(Collectors.toMap(Material::getId, material -> material));

        Requisicao requisicao = new Requisicao();
        requisicao.setOrigem(OrigemRequisicao.FALTA_ESTOQUE);
        requisicao.setOperadorRegistrador(operador);
        requisicao.setEncarregadoNecessidade(encarregado);
        requisicao.setGerenteDestinatario(gerente);
        requisicao.setContrato(contrato);
        requisicao.setTipo(TipoMovimentacao.RETIRADA);
        requisicao.setStatus(StatusRequisicao.PENDENTE);
        requisicao.setCriadaEm(LocalDateTime.now());
        for (Long id : ids) {
            Material material = porId.get(id);
            int solicitado = solicitados.get(id);
            int disponivel = material.getQuantidadeEstoque() == null ? 0 : material.getQuantidadeEstoque();
            int faltante = solicitado - disponivel;
            if (faltante <= 0) continue;
            RequisicaoItem item = new RequisicaoItem();
            item.setDescricao(material.getNome());
            item.setQuantidade(faltante);
            item.setQuantidadeSolicitada(solicitado);
            item.setQuantidadeDisponivel(disponivel);
            item.setQuantidadeFaltante(faltante);
            requisicao.adicionarItem(item);
        }
        if (requisicao.getItens().isEmpty()) {
            throw new RegraNegocioException("Stock is sufficient for the submitted materials");
        }
        return paraResponse(requisicaoRepository.save(requisicao), operador);
    }

    @Transactional(readOnly = true)
    public List<RequisicaoResponse> listar() {
        Usuario usuario = usuarioAutenticadoService.obter();
        List<Requisicao> requisicoes = switch (usuario.getRole()) {
            case ADMIN -> requisicaoRepository.findAllComDetalhes();
            case GERENTE -> requisicaoRepository.findByGerenteSolicitanteIdComDetalhes(usuario.getId());
            case ENCARREGADO -> requisicaoRepository.findByEncarregadoDestinatarioIdComDetalhes(usuario.getId());
            case OPERADOR -> requisicaoRepository.findAllComDetalhes().stream()
                    .filter(requisicao -> usuario.getId().equals(idDe(requisicao.getOperadorRegistrador()))).toList();
            default -> throw new AccessDeniedException("User cannot access requisitions");
        };
        return requisicoes.stream().map(requisicao -> paraResponse(requisicao, usuario)).toList();
    }

    @Transactional(readOnly = true)
    public RequisicaoResponse buscarPorId(Long id) {
        Usuario usuario = usuarioAutenticadoService.obter();
        Requisicao requisicao = buscarComDetalhes(id);
        garantirLeituraPermitida(requisicao, usuario);
        return paraResponse(requisicao, usuario);
    }

    @Transactional
    public RequisicaoResponse visualizar(Long id) {
        Usuario responsavel = usuarioAutenticadoService.obter();
        Requisicao requisicao = buscarComBloqueio(id);
        garantirResponsavelPelaAcao(requisicao, responsavel);
        if (requisicao.getStatus() == StatusRequisicao.PENDENTE) {
            requisicao.setStatus(StatusRequisicao.VISUALIZADA);
            requisicao.setVisualizadaEm(LocalDateTime.now());
        }
        requisicaoRepository.flush();
        return paraResponse(requisicao, responsavel);
    }

    @Transactional
    public RequisicaoResponse concluir(Long id) {
        Usuario responsavel = usuarioAutenticadoService.obter();
        Requisicao requisicao = buscarComBloqueio(id);
        garantirResponsavelPelaAcao(requisicao, responsavel);
        if (requisicao.getStatus() == StatusRequisicao.CANCELADA || requisicao.getStatus() == StatusRequisicao.CONCLUIDA) {
            throw new RegraNegocioException("Requisition cannot be completed in its current status");
        }
        LocalDateTime agora = LocalDateTime.now();
        if (requisicao.getVisualizadaEm() == null) requisicao.setVisualizadaEm(agora);
        requisicao.setStatus(StatusRequisicao.CONCLUIDA);
        requisicao.setConcluidaEm(agora);
        requisicaoRepository.flush();
        return paraResponse(requisicao, responsavel);
    }

    @Transactional
    public RequisicaoResponse cancelar(Long id) {
        Usuario usuario = usuarioAutenticadoService.obter();
        Requisicao requisicao = buscarComBloqueio(id);
        garantirCancelamentoPermitido(requisicao, usuario);
        if (requisicao.getStatus() != StatusRequisicao.PENDENTE && requisicao.getStatus() != StatusRequisicao.VISUALIZADA) {
            throw new RegraNegocioException("Requisition cannot be cancelled in its current status");
        }
        requisicao.setStatus(StatusRequisicao.CANCELADA);
        requisicaoRepository.flush();
        return paraResponse(requisicao, usuario);
    }

    @Transactional
    public RequisicaoResponse alterar(Long id, RequisicaoEdicaoRequest request) {
        Usuario gerente = usuarioAutenticadoService.obter();
        Requisicao requisicao = buscarComBloqueio(id);
        if (gerente.getRole() != Role.GERENTE) {
            throw new AccessDeniedException("Somente o gerente criador pode alterar a requisição");
        }
        if (requisicao.getOrigem() != OrigemRequisicao.MANUAL || requisicao.getTipo() != TipoMovimentacao.RETIRADA) {
            throw new RegraNegocioException("Somente requisições manuais de retirada podem ser alteradas");
        }
        if (!gerenteCriador(requisicao, gerente)) {
            throw new AccessDeniedException("Somente o gerente criador pode alterar a requisição");
        }
        if (requisicao.getStatus() != StatusRequisicao.PENDENTE) {
            String mensagem = switch (requisicao.getStatus()) {
                case VISUALIZADA -> "A requisição não pode mais ser alterada porque já foi visualizada pelo destinatário.";
                case CONCLUIDA -> "A requisição concluída não pode ser alterada.";
                case CANCELADA -> "A requisição cancelada não pode ser alterada.";
                default -> "A requisição não pode ser alterada no estado atual.";
            };
            throw new RegraNegocioException(mensagem);
        }
        if (!Objects.equals(request.getVersao(), requisicao.getVersao())) {
            throw new RegraNegocioException("A requisição foi atualizada por outra edição. Confira os dados atuais antes de alterar novamente.");
        }

        Map<Long, RequisicaoItem> existentes = requisicao.getItens().stream()
                .collect(Collectors.toMap(RequisicaoItem::getId, item -> item));
        var removidos = new HashSet<>(request.getItensRemovidos());
        if (removidos.size() != request.getItensRemovidos().size()
                || !existentes.keySet().containsAll(removidos)
                || !existentes.keySet().containsAll(request.getItensAlterados().keySet())
                || request.getItensAlterados().keySet().stream().anyMatch(removidos::contains)) {
            throw new RegraNegocioException("Informe alterações e remoções de itens distintos pertencentes a esta requisição");
        }
        int total = existentes.size() - removidos.size() + request.getNovosItens().size();
        if (total < 1 || total > 100) {
            throw new RegraNegocioException("A requisição deve conter entre 1 e 100 itens");
        }
        if (request.observacaoFoiInformada()) requisicao.setObservacao(normalizarObservacao(request.getObservacao()));
        request.getItensAlterados().forEach((itemId, alteracao) -> {
            RequisicaoItemRequest conteudo = alteracao.getConteudo();
            RequisicaoItem item = existentes.get(itemId);
            item.setDescricao(conteudo.descricao().trim());
            item.setQuantidade(conteudo.quantidade());
        });
        requisicao.getItens().removeIf(item -> removidos.contains(item.getId()));
        for (RequisicaoEdicaoRequest.Item novo : request.getNovosItens()) {
            RequisicaoItem item = new RequisicaoItem();
            item.setDescricao(novo.descricao().trim());
            item.setQuantidade(novo.quantidade());
            requisicao.adicionarItem(item);
        }
        requisicao.setAtualizadaEm(LocalDateTime.now());
        requisicao.setAtualizadaPor(gerente);
        requisicaoRepository.flush();
        return paraResponse(requisicao, gerente);
    }

    private Requisicao buscarComBloqueio(Long id) {
        return requisicaoRepository.findByIdComBloqueio(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Requisition not found"));
    }

    private Requisicao buscarComDetalhes(Long id) {
        return requisicaoRepository.findByIdComDetalhes(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Requisition not found"));
    }

    private void garantirLeituraPermitida(Requisicao requisicao, Usuario usuario) {
        if (usuario.getRole() == Role.ADMIN) return;
        if (usuario.getRole() == Role.GERENTE && (usuario.getId().equals(idDe(requisicao.getGerenteSolicitante()))
                || usuario.getId().equals(idDe(requisicao.getGerenteDestinatario())))) return;
        if (usuario.getRole() == Role.ENCARREGADO && (usuario.getId().equals(idDe(requisicao.getEncarregadoDestinatario()))
                || usuario.getId().equals(idDe(requisicao.getEncarregadoNecessidade())))) return;
        if (usuario.getRole() == Role.OPERADOR && usuario.getId().equals(idDe(requisicao.getOperadorRegistrador()))) return;
        throw new AccessDeniedException("User cannot access this requisition");
    }

    private void garantirResponsavelPelaAcao(Requisicao requisicao, Usuario usuario) {
        if (!responsavelPelaAcao(requisicao, usuario)) {
            throw new AccessDeniedException("Requisition is assigned to another user");
        }
    }

    private boolean responsavelPelaAcao(Requisicao requisicao, Usuario usuario) {
        boolean manual = requisicao.getOrigem() != OrigemRequisicao.FALTA_ESTOQUE;
        Long responsavelId = manual ? idDe(requisicao.getEncarregadoDestinatario()) : idDe(requisicao.getGerenteDestinatario());
        Role roleObrigatoria = manual ? Role.ENCARREGADO : Role.GERENTE;
        return usuario.getRole() == roleObrigatoria && usuario.getId().equals(responsavelId);
    }

    private void garantirCancelamentoPermitido(Requisicao requisicao, Usuario usuario) {
        if (usuario.getRole() == Role.ADMIN) return;
        boolean manual = requisicao.getOrigem() != OrigemRequisicao.FALTA_ESTOQUE;
        Long criadorId = manual ? idDe(requisicao.getGerenteSolicitante()) : idDe(requisicao.getOperadorRegistrador());
        Role roleObrigatoria = manual ? Role.GERENTE : Role.OPERADOR;
        if (usuario.getRole() != roleObrigatoria || !usuario.getId().equals(criadorId)) {
            throw new AccessDeniedException("User cannot cancel this requisition");
        }
    }

    private void exigirRole(Usuario usuario, Role role, String mensagem) {
        if (usuario.getRole() != role) throw new AccessDeniedException(mensagem);
    }

    private void validarTipo(TipoMovimentacao tipo) {
        if (tipo != TipoMovimentacao.RETIRADA && tipo != TipoMovimentacao.DEVOLUCAO) {
            throw new RegraNegocioException("The requisition type must be RETIRADA or DEVOLUCAO");
        }
    }

    private Contrato buscarContratoAtivo(Long id) {
        Contrato contrato = contratoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Contract not found"));
        if (!Boolean.TRUE.equals(contrato.getAtivo())) {
            throw new RegraNegocioException("Cannot create a requisition for an inactive contract");
        }
        return contrato;
    }

    private Usuario buscarUsuarioAtivo(Long id, Role role, String mensagemNaoEncontrado) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(mensagemNaoEncontrado));
        if (usuario.getRole() != role || !usuario.isAtivo()) throw new RegraNegocioException(mensagemNaoEncontrado);
        return usuario;
    }

    private String normalizarObservacao(String observacao) {
        return observacao == null || observacao.isBlank() ? null : observacao.trim();
    }

    private Long idDe(Usuario usuario) {
        return usuario == null ? null : usuario.getId();
    }

    private boolean gerenteCriador(Requisicao requisicao, Usuario usuario) {
        return usuario.getRole() == Role.GERENTE && usuario.getId().equals(idDe(requisicao.getGerenteSolicitante()));
    }

    private RequisicaoResponse paraResponse(Requisicao requisicao, Usuario usuario) {
        return new RequisicaoResponse(requisicao.getId(), resumo(requisicao.getGerenteSolicitante()),
                resumo(requisicao.getEncarregadoDestinatario()),
                new RequisicaoResponse.ContratoResumo(requisicao.getContrato().getId(), requisicao.getContrato().getNome()),
                requisicao.getTipo(), requisicao.getObservacao(), requisicao.getStatus(), requisicao.getCriadaEm(),
                requisicao.getVisualizadaEm(), requisicao.getConcluidaEm(), requisicao.getItens().stream()
                .map(item -> new RequisicaoResponse.Item(item.getId(), item.getDescricao(), item.getQuantidade(),
                        item.getQuantidadeSolicitada(), item.getQuantidadeDisponivel(), item.getQuantidadeFaltante())).toList(),
                requisicao.getOrigem(), resumo(requisicao.getOperadorRegistrador()),
                resumo(requisicao.getEncarregadoNecessidade()), resumo(requisicao.getGerenteDestinatario()),
                requisicao.getVersao(), requisicao.getAtualizadaEm(), resumo(requisicao.getAtualizadaPor()),
                gerenteCriador(requisicao, usuario) && requisicao.getOrigem() == OrigemRequisicao.MANUAL
                        && requisicao.getTipo() == TipoMovimentacao.RETIRADA && requisicao.getStatus() == StatusRequisicao.PENDENTE,
                requisicao.getStatus() == StatusRequisicao.PENDENTE && responsavelPelaAcao(requisicao, usuario));
    }

    private RequisicaoResponse.UsuarioResumo resumo(Usuario usuario) {
        return usuario == null ? null : new RequisicaoResponse.UsuarioResumo(usuario.getId(), usuario.getNome());
    }
}
