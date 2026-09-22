package com.Lucca.Projeto1.service;

import com.Lucca.Projeto1.dto.movimentacao.MovimentacaoResponse;
import com.Lucca.Projeto1.dto.requisicao.RequisicaoRequest;
import com.Lucca.Projeto1.dto.requisicao.RequisicaoResponse;
import com.Lucca.Projeto1.exception.RecursoNaoEncontradoException;
import com.Lucca.Projeto1.exception.RegraNegocioException;
import com.Lucca.Projeto1.mapper.MovimentacaoMapper;
import com.Lucca.Projeto1.model.Contrato;
import com.Lucca.Projeto1.model.Movimentacao;
import com.Lucca.Projeto1.model.Requisicao;
import com.Lucca.Projeto1.model.Role;
import com.Lucca.Projeto1.model.StatusRequisicao;
import com.Lucca.Projeto1.model.TipoMovimentacao;
import com.Lucca.Projeto1.model.Usuario;
import com.Lucca.Projeto1.repository.ContratoRepository;
import com.Lucca.Projeto1.repository.MovimentacaoRepository;
import com.Lucca.Projeto1.repository.RequisicaoRepository;
import com.Lucca.Projeto1.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RequisicaoService {
    private final RequisicaoRepository requisicaoRepository;
    private final MovimentacaoRepository movimentacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ContratoRepository contratoRepository;
    private final UsuarioAutenticadoService usuarioAutenticadoService;

    public RequisicaoService(
            RequisicaoRepository requisicaoRepository,
            MovimentacaoRepository movimentacaoRepository,
            UsuarioRepository usuarioRepository,
            ContratoRepository contratoRepository,
            UsuarioAutenticadoService usuarioAutenticadoService
    ) {
        this.requisicaoRepository = requisicaoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.contratoRepository = contratoRepository;
        this.usuarioAutenticadoService = usuarioAutenticadoService;
    }

    @Transactional
    public RequisicaoResponse criar(RequisicaoRequest request) {
        Usuario criador = usuarioAutenticadoService.obter();
        if (criador.getRole() != Role.GERENTE && criador.getRole() != Role.ADMIN) {
            throw new RegraNegocioException("Somente gerente ou administrador pode criar requisições");
        }

        Usuario encarregado = usuarioRepository.findById(request.getEncarregadoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Encarregado não encontrado"));
        validarEncarregadoOperacional(encarregado);

        Contrato contrato = contratoRepository.findById(request.getContratoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Contrato não encontrado"));
        if (!Boolean.TRUE.equals(contrato.getAtivo())) {
            throw new RegraNegocioException("A requisição exige um contrato ativo");
        }

        Requisicao requisicao = new Requisicao();
        requisicao.setDescricao(normalizarDescricao(request.getDescricao()));
        requisicao.setGerente(criador);
        requisicao.setEncarregado(encarregado);
        requisicao.setContrato(contrato);
        requisicao.setStatus(StatusRequisicao.PENDENTE);
        requisicao.setCriadoEm(LocalDateTime.now());
        return paraResponse(requisicaoRepository.save(requisicao));
    }

    @Transactional(readOnly = true)
    public List<RequisicaoResponse> listarAdministrativas() {
        Usuario usuario = usuarioAutenticadoService.obter();
        List<Requisicao> requisicoes = usuario.getRole() == Role.ADMIN
                ? requisicaoRepository.findAllByOrderByCriadoEmDesc()
                : requisicaoRepository.findByGerenteIdOrderByCriadoEmDesc(usuario.getId());
        return requisicoes.stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<RequisicaoResponse> listarMinhas() {
        Usuario usuario = usuarioAutenticadoService.obter();
        if (usuario.getRole() != Role.ENCARREGADO) {
            throw new RegraNegocioException("A consulta é exclusiva para encarregados");
        }
        return requisicaoRepository.findByEncarregadoIdOrderByCriadoEmDesc(usuario.getId())
                .stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<RequisicaoResponse> listarPendentes() {
        return requisicaoRepository.findByStatusOrderByCriadoEmAsc(StatusRequisicao.PENDENTE)
                .stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public RequisicaoResponse buscarPorId(Long id) {
        Requisicao requisicao = buscar(id);
        validarAcesso(requisicao, usuarioAutenticadoService.obter());
        return paraResponse(requisicao);
    }

    @Transactional
    public RequisicaoResponse finalizarAtendimento(Long id) {
        Requisicao requisicao = buscarComBloqueio(id);
        if (requisicao.getStatus() != StatusRequisicao.PENDENTE) {
            throw new RegraNegocioException("Somente requisição pendente pode ser finalizada");
        }

        List<Movimentacao> movimentos = movimentacaoRepository.findByRequisicaoIdOrderByIdAsc(id);
        List<Movimentacao> retiradasValidas = movimentos.stream()
                .filter(m -> m.getTipo() == TipoMovimentacao.RETIRADA)
                .filter(m -> movimentos.stream().noneMatch(e ->
                        e.getTipo() == TipoMovimentacao.ESTORNO_RETIRADA
                                && e.getMovimentacaoOrigem() != null
                                && e.getMovimentacaoOrigem().getId().equals(m.getId())))
                .toList();
        if (retiradasValidas.isEmpty()) {
            throw new RegraNegocioException("É necessária ao menos uma retirada válida para finalizar");
        }
        boolean vinculosInvalidos = retiradasValidas.stream().anyMatch(m ->
                !m.getContrato().getId().equals(requisicao.getContrato().getId())
                        || !m.getEncarregado().getId().equals(requisicao.getEncarregado().getId()));
        if (vinculosInvalidos) {
            throw new RegraNegocioException("As retiradas não correspondem ao contrato e encarregado da requisição");
        }
        requisicao.setStatus(StatusRequisicao.AGUARDANDO_CONFIRMACAO);
        return paraResponse(requisicaoRepository.save(requisicao));
    }

    @Transactional
    public RequisicaoResponse confirmar(Long id) {
        Usuario usuario = usuarioAutenticadoService.obter();
        if (usuario.getRole() != Role.ENCARREGADO) {
            throw new RegraNegocioException("Somente encarregado pode confirmar recebimento");
        }
        Requisicao requisicao = buscarComBloqueio(id);
        if (!requisicao.getEncarregado().getId().equals(usuario.getId())) {
            throw new RegraNegocioException("A requisição não pertence ao encarregado autenticado");
        }
        if (requisicao.getStatus() != StatusRequisicao.AGUARDANDO_CONFIRMACAO) {
            throw new RegraNegocioException("A requisição não aguarda confirmação");
        }
        requisicao.setStatus(StatusRequisicao.CONCLUIDA);
        requisicao.setConfirmadoEm(LocalDateTime.now());
        requisicao.setConfirmadoPor(usuario);
        return paraResponse(requisicaoRepository.save(requisicao));
    }

    private void validarAcesso(Requisicao requisicao, Usuario usuario) {
        boolean permitido = switch (usuario.getRole()) {
            case ADMIN -> true;
            case GERENTE -> requisicao.getGerente().getId().equals(usuario.getId());
            case ENCARREGADO -> requisicao.getEncarregado().getId().equals(usuario.getId());
            case OPERADOR -> requisicao.getStatus() != StatusRequisicao.CONCLUIDA;
        };
        if (!permitido) {
            throw new RegraNegocioException("Usuário sem acesso a esta requisição");
        }
    }

    private Requisicao buscar(Long id) {
        return requisicaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Requisição não encontrada"));
    }

    private Requisicao buscarComBloqueio(Long id) {
        return requisicaoRepository.findByIdComBloqueio(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Requisição não encontrada"));
    }

    private void validarEncarregadoOperacional(Usuario encarregado) {
        if (encarregado.getRole() != Role.ENCARREGADO || !encarregado.isAtivo()) {
            throw new RegraNegocioException("O encarregado deve estar ativo e possuir perfil ENCARREGADO");
        }
    }

    private String normalizarDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            throw new RegraNegocioException("A descrição é obrigatória");
        }
        return descricao.trim();
    }

    private RequisicaoResponse paraResponse(Requisicao requisicao) {
        List<Movimentacao> movimentos = movimentacaoRepository
                .findByRequisicaoIdOrderByIdAsc(requisicao.getId());
        Map<Long, Long> estornoPorOrigem = movimentos.stream()
                .filter(m -> m.getMovimentacaoOrigem() != null)
                .collect(Collectors.toMap(m -> m.getMovimentacaoOrigem().getId(), Movimentacao::getId));
        List<MovimentacaoResponse> respostas = movimentos.stream()
                .map(m -> MovimentacaoMapper.paraResponse(m, estornoPorOrigem.get(m.getId())))
                .toList();
        return new RequisicaoResponse(
                requisicao.getId(),
                requisicao.getDescricao(),
                usuarioResumo(requisicao.getGerente()),
                usuarioResumo(requisicao.getEncarregado()),
                new RequisicaoResponse.ContratoResumo(
                        requisicao.getContrato().getId(),
                        requisicao.getContrato().getNome(),
                        requisicao.getContrato().getDescricao()),
                requisicao.getStatus(),
                requisicao.getCriadoEm(),
                requisicao.getConfirmadoEm(),
                requisicao.getConfirmadoPor() == null ? null : usuarioResumo(requisicao.getConfirmadoPor()),
                respostas
        );
    }

    private RequisicaoResponse.UsuarioResumo usuarioResumo(Usuario usuario) {
        return new RequisicaoResponse.UsuarioResumo(usuario.getId(), usuario.getNome(), usuario.getUsername());
    }
}
