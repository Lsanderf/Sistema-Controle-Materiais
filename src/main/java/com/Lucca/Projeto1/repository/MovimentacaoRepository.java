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
    List<Movimentacao> findByEncarregadoId(Long encarregadoId);
    List<Movimentacao> findByMaterialId(Long materialId);
    List<Movimentacao> findByContratoId(Long contratoId);
    List<Movimentacao> findByEncarregadoIdAndContratoIdAndMaterialId(
            Long encarregadoId,
            Long contratoId,
            Long materialId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "material",
            "registradoPor",
            "encarregado",
            "contrato",
            "notaFiscal",
            "movimentacaoOrigem",
            "requisicao"
    })
    @Query("SELECT movimentacao FROM Movimentacao movimentacao WHERE movimentacao.id = :id")
    Optional<Movimentacao> findByIdComBloqueio(@Param("id") Long id);

    @EntityGraph(attributePaths = {
            "material",
            "registradoPor",
            "encarregado",
            "contrato",
            "notaFiscal",
            "movimentacaoOrigem",
            "requisicao"
    })
    List<Movimentacao> findByNotaFiscalIdOrderByIdAsc(Long notaFiscalId);

    @EntityGraph(attributePaths = {
            "material",
            "registradoPor",
            "encarregado",
            "contrato",
            "notaFiscal",
            "movimentacaoOrigem",
            "requisicao"
    })
    List<Movimentacao> findByNotaFiscalIdInOrderByNotaFiscalIdAscIdAsc(
            List<Long> notaFiscalIds
    );

    @EntityGraph(attributePaths = {
            "material",
            "registradoPor",
            "encarregado",
            "contrato",
            "notaFiscal",
            "movimentacaoOrigem",
            "requisicao"
    })
    Optional<Movimentacao> findByRegistradoPorIdAndIdempotencyKey(
            Long usuarioId,
            String idempotencyKey
    );

    @EntityGraph(attributePaths = {
            "material",
            "registradoPor",
            "encarregado",
            "contrato",
            "notaFiscal",
            "movimentacaoOrigem",
            "requisicao"
    })
    Optional<Movimentacao> findByMovimentacaoOrigemId(Long movimentacaoOrigemId);

    List<Movimentacao> findByMovimentacaoOrigemIdIn(
            Collection<Long> movimentacaoOrigemIds
    );

    @EntityGraph(attributePaths = {
            "material", "registradoPor", "encarregado", "contrato",
            "notaFiscal", "movimentacaoOrigem", "requisicao"
    })
    List<Movimentacao> findByRequisicaoIdOrderByIdAsc(Long requisicaoId);

    @Query("""
            SELECT COUNT(m)
            FROM Movimentacao m
            WHERE m.requisicao.id = :requisicaoId
              AND m.tipo = com.Lucca.Projeto1.model.TipoMovimentacao.RETIRADA
              AND NOT EXISTS (
                  SELECT 1 FROM Movimentacao e
                  WHERE e.movimentacaoOrigem = m
                    AND e.tipo = com.Lucca.Projeto1.model.TipoMovimentacao.ESTORNO_RETIRADA
              )
            """)
    long contarRetiradasValidas(@Param("requisicaoId") Long requisicaoId);
}
