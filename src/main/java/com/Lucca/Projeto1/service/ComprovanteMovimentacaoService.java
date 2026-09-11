package com.Lucca.Projeto1.service;

import com.Lucca.Projeto1.dto.movimentacao.ComprovanteMovimentacaoResponse;
import com.Lucca.Projeto1.dto.movimentacao.EvidenciaMovimentacaoResponse;
import com.Lucca.Projeto1.exception.RecursoNaoEncontradoException;
import com.Lucca.Projeto1.exception.RegraNegocioException;
import com.Lucca.Projeto1.mapper.ComprovanteMovimentacaoMapper;
import com.Lucca.Projeto1.mapper.EvidenciaMovimentacaoMapper;
import com.Lucca.Projeto1.model.ComprovanteMovimentacao;
import com.Lucca.Projeto1.model.Movimentacao;
import com.Lucca.Projeto1.repository.ComprovanteMovimentacaoRepository;
import com.Lucca.Projeto1.repository.EvidenciaMovimentacaoRepository;
import com.Lucca.Projeto1.repository.MovimentacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
public class ComprovanteMovimentacaoService {

    private final ComprovanteMovimentacaoRepository comprovanteRepository;
    private final EvidenciaMovimentacaoRepository evidenciaRepository;
    private final MovimentacaoRepository movimentacaoRepository;

    public ComprovanteMovimentacaoService(
            ComprovanteMovimentacaoRepository comprovanteRepository,
            EvidenciaMovimentacaoRepository evidenciaRepository,
            MovimentacaoRepository movimentacaoRepository
    ) {
        this.comprovanteRepository = comprovanteRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    @Transactional
    public void registrar(Movimentacao movimentacao) {
        comprovanteRepository.save(
                ComprovanteMovimentacao.registrar(movimentacao)
        );
    }

    @Transactional
    public void registrarTodos(Collection<Movimentacao> movimentacoes) {
        comprovanteRepository.saveAll(
                movimentacoes.stream()
                        .map(ComprovanteMovimentacao::registrar)
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public ComprovanteMovimentacaoResponse buscar(Long movimentacaoId) {
        ComprovanteMovimentacao comprovante = comprovanteRepository
                .findById(movimentacaoId)
                .orElseThrow(() -> comprovanteNaoEncontrado(movimentacaoId));

        List<EvidenciaMovimentacaoResponse> evidencias = evidenciaRepository
                .findByMovimentacaoIdOrderByDataEvidenciaAsc(movimentacaoId)
                .stream()
                .map(EvidenciaMovimentacaoMapper::paraResponse)
                .toList();

        return ComprovanteMovimentacaoMapper.paraResponse(
                comprovante,
                evidencias
        );
    }

    private RuntimeException comprovanteNaoEncontrado(Long movimentacaoId) {
        if (!movimentacaoRepository.existsById(movimentacaoId)) {
            return new RecursoNaoEncontradoException(
                    "Movimentação com ID " + movimentacaoId + " não encontrada"
            );
        }

        return new RegraNegocioException(
                "A movimentação existe, mas seu comprovante não foi gerado"
        );
    }
}
