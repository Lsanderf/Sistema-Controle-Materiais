package com.Lucca.Projeto1.repository;

import com.Lucca.Projeto1.model.SolicitacaoRetirada;
import com.Lucca.Projeto1.model.StatusSolicitacaoRetirada;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SolicitacaoRetiradaRepository extends JpaRepository<SolicitacaoRetirada, Long> {
    Optional<SolicitacaoRetirada> findByOperadorResponsavelIdAndIdempotencyKey(Long operadorId, String idempotencyKey);

    boolean existsByEncarregadoAssinanteIdAndStatus(Long encarregadoId, StatusSolicitacaoRetirada status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"operadorResponsavel", "encarregadoAssinante", "contrato", "itens", "itens.material"})
    @Query("select solicitacao from SolicitacaoRetirada solicitacao where solicitacao.id = :id")
    Optional<SolicitacaoRetirada> findByIdComBloqueio(@Param("id") Long id);
}
