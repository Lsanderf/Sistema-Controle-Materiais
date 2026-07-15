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
            throw new IllegalArgumentException("A quantidade deve ser mair que 0");
        }
        if(request.getTipo() == null){
            throw new IllegalArgumentException("O tipo da movimentação é obrigatoria!");
        }

        Funcionario funcionario = funcionarioRepository.
                findById(request.getFuncionarioId()).orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Funcionário não encontrado"));
        Contrato contrato = contratoRepository
                .findById(request.getContratoId())
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Contrato não encontrado"));

        Material material = materialRepository.findById(request.getMaterialId()).orElseThrow(() ->
                new RecursoNaoEncontradoException(
                        "Material não encontrado!"));

        if(request.getTipo() == TipoMovimentacao.RETIRADA){
            if(material.getQuantidadeEstoque() < request.getQuantidade() || material.getQuantidadeEstoque() == null){
                throw new RegraNegocioException(
                        "Quantidade insuficiente em estoque"
                );
            }
            material.setQuantidadeEstoque(
                    material.getQuantidadeEstoque()
                            - request.getQuantidade());
        }else if(request.getTipo() == TipoMovimentacao.DEVOLUCAO){
            material.setQuantidadeEstoque(
                    material.getQuantidadeEstoque()
                            + request.getQuantidade());
        }
        if (!Boolean.TRUE.equals(contrato.getAtivo())) {
            throw new RegraNegocioException(
                    "Não é possível registrar movimentações em um contrato inativo"
            );
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

    public List<Movimentacao> listarPorFuncionario(
            Long funcionarioId
    ) {
        return movimentacaoRepository
                .findByFuncionarioId(funcionarioId);
    }

    public List<Movimentacao> listarPorContrato(
            Long contratoId
    ) {
        return movimentacaoRepository
                .findByContratoId(contratoId);
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

    public List<Movimentacao> listarPorMaterial(
            Long materialId
    ) {
        return movimentacaoRepository
                .findByMaterialId(materialId);
    }
}
