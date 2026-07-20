package com.Lucca.Projeto1.service;

import com.Lucca.Projeto1.dto.MovimentacaoRequest;
import com.Lucca.Projeto1.dto.MovimentacaoResponse;
import com.Lucca.Projeto1.model.*;
import com.Lucca.Projeto1.repository.ContratoRepository;
import com.Lucca.Projeto1.repository.FuncionarioRepository;
import com.Lucca.Projeto1.repository.MaterialRepository;
import com.Lucca.Projeto1.repository.MovimentacaoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import com.Lucca.Projeto1.exception.RecursoNaoEncontradoException;
import com.Lucca.Projeto1.exception.RegraNegocioException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MovimentacaoService {

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
    public MovimentacaoResponse registrarMovimentacao( MovimentacaoRequest request){
        if(request.getQuantidade() == null || request.getQuantidade() <= 0 ) {
            throw new RegraNegocioException("A quantidade deve ser maior que 0");
        }
        if(request.getTipo() == null){
            throw new RegraNegocioException("O tipo da movimentacao e obrigatorio!");
        }

        Funcionario funcionario = funcionarioRepository.
                findById(request.getFuncionarioId()).orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Funcionario nao encontrado"));
        Contrato contrato = contratoRepository
                .findById(request.getContratoId())
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Contrato nao encontrado"));

        Material material = materialRepository.findById(request.getMaterialId()).orElseThrow(() ->
                new RecursoNaoEncontradoException(
                        "Material nao encontrado!"));

        if (!Boolean.TRUE.equals(contrato.getAtivo())) {
            throw new RegraNegocioException(
                    "Nao e possivel registrar movimentacoes em um contrato inativo"
            );
        }

        int estoqueAtual = material.getQuantidadeEstoque() == null
                ? 0
                : material.getQuantidadeEstoque();

        if(request.getTipo() == TipoMovimentacao.RETIRADA){
            if(estoqueAtual < request.getQuantidade()){
                throw new RegraNegocioException(
                        "Quantidade insuficiente em estoque"
                );
            }
            material.setQuantidadeEstoque(
                    estoqueAtual
                            - request.getQuantidade());
        }else if(request.getTipo() == TipoMovimentacao.DEVOLUCAO){
            int quantidadeAindaRetirada = calcularQuantidadeAindaRetirada(
                    request.getFuncionarioId(),
                    request.getContratoId(),
                    request.getMaterialId()
            );

            if (request.getQuantidade() > quantidadeAindaRetirada) {
                throw new RegraNegocioException(
                        "A devolucao nao pode ser maior que a quantidade ainda retirada"
                );
            }

            material.setQuantidadeEstoque(
                    estoqueAtual
                            + request.getQuantidade());
        }
        materialRepository.save(material);

        Movimentacao movimentacao = new Movimentacao();

        movimentacao.setFuncionario(funcionario);
        movimentacao.setContrato(contrato);
        movimentacao.setMaterial(material);
        movimentacao.setQuantidade(request.getQuantidade());
        movimentacao.setTipo(request.getTipo());
        movimentacao.setDataMovimentacao(LocalDateTime.now());

        Movimentacao movimentacaoSalva =
                movimentacaoRepository.save(movimentacao);

        return converterParaResponse(movimentacaoSalva);
    }

    public List<MovimentacaoResponse> listarTodas() {
        return movimentacaoRepository.findAll()
                .stream()
                .map(this::converterParaResponse)
                .toList();
    }

    public List<MovimentacaoResponse> listarPorFuncionario(
            Long funcionarioId
    ) {
        return movimentacaoRepository
                .findByFuncionarioId(funcionarioId)
                .stream()
                .map(this::converterParaResponse)
                .toList();
    }

    public List<MovimentacaoResponse> listarPorContrato(
            Long contratoId
    ) {
        return movimentacaoRepository
                .findByContratoId(contratoId)
                .stream()
                .map(this::converterParaResponse)
                .toList();
    }

    private MovimentacaoResponse converterParaResponse(Movimentacao movimentacao) {
        return new MovimentacaoResponse(
                movimentacao.getId(),
                movimentacao.getFuncionario().getNome(),
                movimentacao.getContrato().getNome(),
                movimentacao.getMaterial().getNome(),
                movimentacao.getQuantidade(),
                movimentacao.getTipo(),
                movimentacao.getDataMovimentacao()
        );
    }

    public List<MovimentacaoResponse> listarPorMaterial(
            Long materialId
    ) {
        return movimentacaoRepository
                .findByMaterialId(materialId)
                .stream()
                .map(this::converterParaResponse)
                .toList();
    }

    private int calcularQuantidadeAindaRetirada(
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
                .mapToInt(movimentacao -> {
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
