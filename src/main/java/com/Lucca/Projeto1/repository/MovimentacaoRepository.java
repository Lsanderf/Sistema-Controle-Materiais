package com.Lucca.Projeto1.repository;
import com.Lucca.Projeto1.model.Movimentacao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Long> {
    List<Movimentacao> findByFuncionarioId(Long funcionarioId);
    List<Movimentacao> findByMaterialId(Long materialId);
    List<Movimentacao> findByContratoId(Long contratoId);
    List<Movimentacao> findByFuncionarioIdAndContratoIdAndMaterialId(
            Long funcionarioId,
            Long contratoId,
            Long materialId
    );

    @EntityGraph(attributePaths = {
            "material",
            "registradoPor",
            "funcionario",
            "contrato",
            "notaFiscal"
    })
    List<Movimentacao> findByNotaFiscalIdOrderByIdAsc(Long notaFiscalId);

    @EntityGraph(attributePaths = {
            "material",
            "registradoPor",
            "funcionario",
            "contrato",
            "notaFiscal"
    })
    List<Movimentacao> findByNotaFiscalIdInOrderByNotaFiscalIdAscIdAsc(
            List<Long> notaFiscalIds
    );

}
