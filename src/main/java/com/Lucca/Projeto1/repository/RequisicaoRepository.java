package com.Lucca.Projeto1.repository;

import com.Lucca.Projeto1.model.Requisicao;
import com.Lucca.Projeto1.model.StatusRequisicao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RequisicaoRepository extends JpaRepository<Requisicao, Long> {

    @EntityGraph(attributePaths = {"gerente", "encarregado", "contrato", "confirmadoPor"})
    List<Requisicao> findAllByOrderByCriadoEmDesc();

    @EntityGraph(attributePaths = {"gerente", "encarregado", "contrato", "confirmadoPor"})
    List<Requisicao> findByGerenteIdOrderByCriadoEmDesc(Long gerenteId);

    @EntityGraph(attributePaths = {"gerente", "encarregado", "contrato", "confirmadoPor"})
    List<Requisicao> findByEncarregadoIdOrderByCriadoEmDesc(Long encarregadoId);

    @EntityGraph(attributePaths = {"gerente", "encarregado", "contrato", "confirmadoPor"})
    List<Requisicao> findByStatusOrderByCriadoEmAsc(StatusRequisicao status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"gerente", "encarregado", "contrato", "confirmadoPor"})
    @Query("SELECT r FROM Requisicao r WHERE r.id = :id")
    Optional<Requisicao> findByIdComBloqueio(@Param("id") Long id);
}
