package com.Lucca.Projeto1.service;

import com.Lucca.Projeto1.dto.requisicao.RequisicaoItemRequest;
import com.Lucca.Projeto1.dto.requisicao.RequisicaoRequest;
import com.Lucca.Projeto1.dto.requisicao.RequisicaoResponse;
import com.Lucca.Projeto1.exception.RecursoNaoEncontradoException;
import com.Lucca.Projeto1.exception.RegraNegocioException;
import com.Lucca.Projeto1.model.Contrato;
import com.Lucca.Projeto1.model.Material;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RequisicaoService {

    private final RequisicaoRepository requisicaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ContratoRepository contratoRepository;
    private final MaterialRepository materialRepository;
    private final UsuarioAutenticadoService usuarioAutenticadoService;

    public RequisicaoService(RequisicaoRepository requisicaoRepository, UsuarioRepository usuarioRepository,
                             ContratoRepository contratoRepository, MaterialRepository materialRepository,
                             UsuarioAutenticadoService usuarioAutenticadoService) {
        this.requisicaoRepository = requisicaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.contratoRepository = contratoRepository;
        this.materialRepository = materialRepository;
        this.usuarioAutenticadoService = usuarioAutenticadoService;
    }

    @Transactional
    public RequisicaoResponse criar(RequisicaoRequest request) {
        Usuario gerente = usuarioAutenticadoService.obter();
        exigirRole(gerente, Role.GERENTE, "Apenas gerente pode criar requisições");
        validarTipo(request.tipo());

        Usuario encarregado = usuarioRepository.findById(request.encarregadoDestinatarioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Encarregado não encontrado"));
        if (encarregado.getRole() != Role.ENCARREGADO || !encarregado.isAtivo()) {
            throw new RegraNegocioException("O destinatário deve ser um encarregado ativo");
        }
        Contrato contrato = contratoRepository.findById(request.contratoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Contrato não encontrado"));
        if (!Boolean.TRUE.equals(contrato.getAtivo())) {
            throw new RegraNegocioException("Não é possível requisitar para um contrato inativo");
        }

        Requisicao requisicao = new Requisicao();
        requisicao.setGerenteSolicitante(gerente);
        requisicao.setEncarregadoDestinatario(encarregado);
        requisicao.setContrato(contrato);
        requisicao.setTipo(request.tipo());
        requisicao.setObservacao(normalizarObservacao(request.observacao()));
        requisicao.setStatus(StatusRequisicao.PENDENTE);
        requisicao.setCriadaEm(LocalDateTime.now());

        Set<Long> materiaisIncluidos = new HashSet<>();
        for (RequisicaoItemRequest itemRequest : request.itens()) {
            if (!materiaisIncluidos.add(itemRequest.materialId())) {
                throw new RegraNegocioException("Um material não pode ser informado mais de uma vez na requisição");
            }
            Material material = materialRepository.findById(itemRequest.materialId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Material não encontrado"));
            RequisicaoItem item = new RequisicaoItem();
            item.setMaterial(material);
            item.setQuantidade(itemRequest.quantidade());
            requisicao.adicionarItem(item);
        }

        return paraResponse(requisicaoRepository.save(requisicao));
    }

    @Transactional(readOnly = true)
    public List<RequisicaoResponse> listar() {
        Usuario usuario = usuarioAutenticadoService.obter();
        List<Requisicao> requisicoes = switch (usuario.getRole()) {
            case ADMIN -> requisicaoRepository.findAllComDetalhes();
            case GERENTE -> requisicaoRepository.findByGerenteSolicitanteIdComDetalhes(usuario.getId());
            case ENCARREGADO -> requisicaoRepository.findByEncarregadoDestinatarioIdComDetalhes(usuario.getId());
            default -> throw new AccessDeniedException("Usuário sem acesso a requisições");
        };
        return requisicoes.stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public RequisicaoResponse buscarPorId(Long id) {
        Usuario usuario = usuarioAutenticadoService.obter();
        Requisicao requisicao = buscarComDetalhes(id);
        garantirLeituraPermitida(requisicao, usuario);
        return paraResponse(requisicao);
    }

    @Transactional
    public RequisicaoResponse visualizar(Long id) {
        Usuario encarregado = usuarioAutenticadoService.obter();
        exigirRole(encarregado, Role.ENCARREGADO, "Apenas o encarregado destinatário pode visualizar a requisição");
        Requisicao requisicao = buscarComDetalhes(id);
        garantirDestinatario(requisicao, encarregado);
        if (requisicao.getStatus() == StatusRequisicao.PENDENTE) {
            requisicao.setStatus(StatusRequisicao.VISUALIZADA);
            requisicao.setVisualizadaEm(LocalDateTime.now());
        }
        return paraResponse(requisicao);
    }

    @Transactional
    public RequisicaoResponse concluir(Long id) {
        Usuario encarregado = usuarioAutenticadoService.obter();
        exigirRole(encarregado, Role.ENCARREGADO, "Apenas o encarregado destinatário pode concluir a requisição");
        Requisicao requisicao = buscarComDetalhes(id);
        garantirDestinatario(requisicao, encarregado);
        if (requisicao.getStatus() == StatusRequisicao.CANCELADA || requisicao.getStatus() == StatusRequisicao.CONCLUIDA) {
            throw new RegraNegocioException("A requisição não pode ser concluída no estado atual");
        }
        LocalDateTime agora = LocalDateTime.now();
        if (requisicao.getVisualizadaEm() == null) requisicao.setVisualizadaEm(agora);
        requisicao.setStatus(StatusRequisicao.CONCLUIDA);
        requisicao.setConcluidaEm(agora);
        return paraResponse(requisicao);
    }

    @Transactional
    public RequisicaoResponse cancelar(Long id) {
        Usuario gerente = usuarioAutenticadoService.obter();
        exigirRole(gerente, Role.GERENTE, "Apenas o gerente solicitante pode cancelar a requisição");
        Requisicao requisicao = buscarComDetalhes(id);
        if (!requisicao.getGerenteSolicitante().getId().equals(gerente.getId())) {
            throw new RegraNegocioException("Você não pode cancelar a requisição de outro gerente");
        }
        if (requisicao.getStatus() != StatusRequisicao.PENDENTE && requisicao.getStatus() != StatusRequisicao.VISUALIZADA) {
            throw new RegraNegocioException("A requisição não pode ser cancelada no estado atual");
        }
        requisicao.setStatus(StatusRequisicao.CANCELADA);
        return paraResponse(requisicao);
    }

    private Requisicao buscarComDetalhes(Long id) {
        return requisicaoRepository.findByIdComDetalhes(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Requisição não encontrada"));
    }

    private void garantirLeituraPermitida(Requisicao requisicao, Usuario usuario) {
        if (usuario.getRole() == Role.ADMIN) return;
        if (usuario.getRole() == Role.GERENTE && requisicao.getGerenteSolicitante().getId().equals(usuario.getId())) return;
        if (usuario.getRole() == Role.ENCARREGADO && requisicao.getEncarregadoDestinatario().getId().equals(usuario.getId())) return;
        throw new AccessDeniedException("Você não tem acesso a esta requisição");
    }

    private void garantirDestinatario(Requisicao requisicao, Usuario encarregado) {
        if (!requisicao.getEncarregadoDestinatario().getId().equals(encarregado.getId())) {
            throw new AccessDeniedException("A requisição é destinada a outro encarregado");
        }
    }

    private void exigirRole(Usuario usuario, Role role, String mensagem) {
        if (usuario.getRole() != role) throw new AccessDeniedException(mensagem);
    }

    private void validarTipo(TipoMovimentacao tipo) {
        if (tipo != TipoMovimentacao.RETIRADA && tipo != TipoMovimentacao.DEVOLUCAO) {
            throw new RegraNegocioException("O tipo de requisição deve ser RETIRADA ou DEVOLUCAO");
        }
    }

    private String normalizarObservacao(String observacao) {
        if (observacao == null || observacao.isBlank()) return null;
        return observacao.trim();
    }

    private RequisicaoResponse paraResponse(Requisicao requisicao) {
        return new RequisicaoResponse(
                requisicao.getId(),
                new RequisicaoResponse.UsuarioResumo(requisicao.getGerenteSolicitante().getId(), requisicao.getGerenteSolicitante().getNome()),
                new RequisicaoResponse.UsuarioResumo(requisicao.getEncarregadoDestinatario().getId(), requisicao.getEncarregadoDestinatario().getNome()),
                new RequisicaoResponse.ContratoResumo(requisicao.getContrato().getId(), requisicao.getContrato().getNome()),
                requisicao.getTipo(), requisicao.getObservacao(), requisicao.getStatus(), requisicao.getCriadaEm(),
                requisicao.getVisualizadaEm(), requisicao.getConcluidaEm(),
                requisicao.getItens().stream().map(item -> new RequisicaoResponse.Item(
                        item.getId(), new RequisicaoResponse.MaterialResumo(item.getMaterial().getId(), item.getMaterial().getNome()), item.getQuantidade()
                )).toList()
        );
    }
}
