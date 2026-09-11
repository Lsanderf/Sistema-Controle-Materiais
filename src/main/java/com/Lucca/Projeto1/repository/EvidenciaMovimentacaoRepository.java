package com.Lucca.Projeto1.repository;

import com.Lucca.Projeto1.model.EvidenciaMovimentacao;
import com.Lucca.Projeto1.model.TipoEvidenciaMovimentacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvidenciaMovimentacaoRepository
        extends JpaRepository<EvidenciaMovimentacao, Long> {

    List<EvidenciaMovimentacao> findByMovimentacaoIdOrderByDataEvidenciaAsc(
            Long movimentacaoId
    );

    boolean existsByMovimentacaoIdAndTipo(
            Long movimentacaoId,
            TipoEvidenciaMovimentacao tipo
    );

    Optional<EvidenciaMovimentacao> findByIdAndMovimentacaoId(
            Long id,
            Long movimentacaoId
    );
}
