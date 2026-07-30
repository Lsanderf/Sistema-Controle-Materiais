package com.Lucca.Projeto1.service;

import com.Lucca.Projeto1.dto.movimentacao.EntradaEstoqueRequest;
import com.Lucca.Projeto1.dto.movimentacao.MovimentacaoRequest;
import com.Lucca.Projeto1.dto.movimentacao.MovimentacaoResponse;
import com.Lucca.Projeto1.exception.RecursoNaoEncontradoException;
import com.Lucca.Projeto1.exception.RegraNegocioException;
import com.Lucca.Projeto1.mapper.MovimentacaoMapper;
import com.Lucca.Projeto1.model.Contrato;
import com.Lucca.Projeto1.model.Funcionario;
import com.Lucca.Projeto1.model.Material;
import com.Lucca.Projeto1.model.Movimentacao;
import com.Lucca.Projeto1.model.TipoMovimentacao;
import com.Lucca.Projeto1.repository.ContratoRepository;
import com.Lucca.Projeto1.repository.FuncionarioRepository;
import com.Lucca.Projeto1.repository.MaterialRepository;
import com.Lucca.Projeto1.repository.MovimentacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MovimentacaoService {

    private static final int LIMITE_QUANTIDADE_OPERACAO = 10000;

    private final MovimentacaoRepository movimentacaoRepository;
    private final ContratoRepository contratoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final MaterialRepository materialRepository;

    public MovimentacaoService(
            MovimentacaoRepository movimentacaoRepository,
            FuncionarioRepository funcionarioRepository,
            ContratoRepository contratoRepository,
            MaterialRepository materialRepository
    ) {
        this.movimentacaoRepository = movimentacaoRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.contratoRepository = contratoRepository;
        this.materialRepository = materialRepository;
    }

    @Transactional
    public MovimentacaoResponse registrarEntrada(
            EntradaEstoqueRequest request
    ) {
        validarQuantidade(request.getQuantidade());

        Material material = buscarMaterialComBloqueio(request.getMaterialId());
        int novoEstoque = somarEstoque(
                estoqueAtual(material),
                request.getQuantidade()
        );

        material.setQuantidadeEstoque(novoEstoque);

        Movimentacao movimentacao = new Movimentacao();
        movimentacao.setMaterial(material);
        movimentacao.setQuantidade(request.getQuantidade());
        movimentacao.setTipo(TipoMovimentacao.ENTRADA);
        movimentacao.setDataMovimentacao(LocalDateTime.now());
        movimentacao.setFuncionario(null);
        movimentacao.setContrato(null);

        Movimentacao movimentacaoSalva =
                movimentacaoRepository.save(movimentacao);

        return MovimentacaoMapper.paraResponse(
                movimentacaoSalva
        );
    }

    @Transactional
    public MovimentacaoResponse registrarMovimentacao(
            MovimentacaoRequest request
    ) {
        validarQuantidade(request.getQuantidade());
        validarTipoMovimentacaoComum(request.getTipo());

        Funcionario funcionario = funcionarioRepository
                .findById(request.getFuncionarioId())
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Funcionário não encontrado"
                        )
                );

        if (!funcionario.isAtivo()) {
            throw new RegraNegocioException(
                    "Não é possível registrar movimentações para um funcionário inativo"
            );
        }

        Contrato contrato = contratoRepository
                .findById(request.getContratoId())
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Contrato não encontrado"
                        )
                );

        if (!Boolean.TRUE.equals(contrato.getAtivo())) {
            throw new RegraNegocioException(
                    "Não é possível registrar movimentações em um contrato inativo"
            );
        }

        Material material = buscarMaterialComBloqueio(request.getMaterialId());
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
        movimentacao.setDataMovimentacao(LocalDateTime.now());

        Movimentacao movimentacaoSalva =
                movimentacaoRepository.save(movimentacao);

        return MovimentacaoMapper.paraResponse(movimentacaoSalva);
    }

    public List<MovimentacaoResponse> listarTodas() {
        return movimentacaoRepository.findAll()
                .stream()
                .map(MovimentacaoMapper::paraResponse)
                .toList();
    }

    public MovimentacaoResponse listarPorId(Long id) {
        return movimentacaoRepository.findById(id)
                .map(MovimentacaoMapper::paraResponse)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Movimentação não encontrada"
                        )
                );
    }

    public List<MovimentacaoResponse> listarPorFuncionario(
            Long funcionarioId
    ) {
        return movimentacaoRepository
                .findByFuncionarioId(funcionarioId)
                .stream()
                .map(MovimentacaoMapper::paraResponse)
                .toList();
    }

    public List<MovimentacaoResponse> listarPorContrato(
            Long contratoId
    ) {
        return movimentacaoRepository
                .findByContratoId(contratoId)
                .stream()
                .map(MovimentacaoMapper::paraResponse)
                .toList();
    }

    public List<MovimentacaoResponse> listarPorMaterial(
            Long materialId
    ) {
        return movimentacaoRepository
                .findByMaterialId(materialId)
                .stream()
                .map(MovimentacaoMapper::paraResponse)
                .toList();
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
                    "Entradas devem ser registradas pelo endpoint /movimentacoes/entrada"
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
                    return 0;
                })
                .sum();
    }
}
