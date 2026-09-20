package com.Lucca.Projeto1.repository;
import com.Lucca.Projeto1.model.Movimentacao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.Collection;


public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Long> {
    List<Movimentacao> findByFuncionarioId(Long funcionarioId);
    List<Movimentacao> findByMaterialId(Long materialId);
    List<Movimentacao> findByContratoId(Long contratoId);
    List<Movimentacao> findByFuncionarioIdAndContratoIdAndMaterialId(
            Long funcionarioId,
            Long contratoId,
            Long materialId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "material",
            "registradoPor",
            "funcionario",
            "contrato",
            "notaFiscal",
            "movimentacaoOrigem"
    })
    @Query("SELECT movimentacao FROM Movimentacao movimentacao WHERE movimentacao.id = :id")
    Optional<Movimentacao> findByIdComBloqueio(@Param("id") Long id);

    @EntityGraph(attributePaths = {
            "material",
            "registradoPor",
            "funcionario",
            "contrato",
            "notaFiscal",
            "movimentacaoOrigem"
    })
    List<Movimentacao> findByNotaFiscalIdOrderByIdAsc(Long notaFiscalId);

    @EntityGraph(attributePaths = {
            "material",
            "registradoPor",
            "funcionario",
            "contrato",
            "notaFiscal",
            "movimentacaoOrigem"
    })
    List<Movimentacao> findByNotaFiscalIdInOrderByNotaFiscalIdAscIdAsc(
            List<Long> notaFiscalIds
    );

    @EntityGraph(attributePaths = {
            "material",
            "registradoPor",
            "funcionario",
            "contrato",
            "notaFiscal",
            "movimentacaoOrigem"
    })
    Optional<Movimentacao> findByRegistradoPorIdAndIdempotencyKey(
            Long usuarioId,
            String idempotencyKey
    );

    @EntityGraph(attributePaths = {
            "material",
            "registradoPor",
            "funcionario",
            "contrato",
            "notaFiscal",
            "movimentacaoOrigem"
    })
    Optional<Movimentacao> findByMovimentacaoOrigemId(Long movimentacaoOrigemId);

    List<Movimentacao> findByMovimentacaoOrigemIdIn(
            Collection<Long> movimentacaoOrigemIds
    );
}
