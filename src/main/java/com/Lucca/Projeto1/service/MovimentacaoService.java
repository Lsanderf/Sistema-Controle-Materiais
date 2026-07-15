package com.Lucca.Projeto1.service;

import com.Lucca.Projeto1.dto.MovimentacaoRequest;
import com.Lucca.Projeto1.model.*;
import com.Lucca.Projeto1.repository.ContratoRepository;
import com.Lucca.Projeto1.repository.FuncionarioRepository;
import com.Lucca.Projeto1.repository.MaterialRepository;
import com.Lucca.Projeto1.repository.MovimentacaoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MovimentacaoService {

    private MovimentacaoRepository movimentacaoRepository;
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
    public Movimentacao registrarMovimentacao(MovimentacaoRequest request){
        if(request.getQuantidade() == null || request.getQuantidade() <= 0 ) {
            throw new IllegalArgumentException("A quantidade deve ser mair que 0");
        }
        if(request.getTipo() == null){
            throw new IllegalArgumentException("O tipo da movimentação é obrigatoria!");
        }

        Funcionario funcionario = funcionarioRepository.
                findById(request.getFuncionarioId()).orElseThrow(() ->
                        new RuntimeException("Funcionario não encontrado!"));
        Contrato contrato = contratoRepository.
                findById(request.getContratoId()).orElseThrow(() ->
                        new RuntimeException("Funcionario não encontrado!"));

        Material material = materialRepository.findById(request.getMaterialId()).orElseThrow(() ->
                new RuntimeException("Material não encontrado!"));

        if(request.getTipo() == TipoMovimentacao.RETIRADA){
            if(material.getQuantidadeEstoque() < request.getQuantidade()){
                throw new IllegalArgumentException(
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
        materialRepository.save(material);

        Movimentacao movimentacao = new Movimentacao();

        movimentacao.setFuncionario(funcionario);
        movimentacao.setContrato(contrato);
        movimentacao.setMaterial(material);
        movimentacao.setQuantidade(request.getQuantidade());
        movimentacao.setTipo(request.getTipo());
        movimentacao.setDataMovimentacao(LocalDateTime.now());

        return movimentacaoRepository.save(movimentacao);
    }

    public List<Movimentacao> listarTodas() {
        return movimentacaoRepository.findAll();
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

    public List<Movimentacao> listarPorMaterial(
            Long materialId
    ) {
        return movimentacaoRepository
                .findByMaterialId(materialId);
    }
}
