package com.Lucca.Projeto1.repository;

import com.Lucca.Projeto1.model.NotaFiscalEntrada;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotaFiscalEntradaRepository
        extends JpaRepository<NotaFiscalEntrada, Long> {

    boolean existsByChaveAcesso(String chaveAcesso);

    boolean existsByChaveAcessoAndIdNot(String chaveAcesso, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT nota FROM NotaFiscalEntrada nota WHERE nota.id = :id")
    Optional<NotaFiscalEntrada> findByIdComBloqueio(@Param("id") Long id);

    @EntityGraph(attributePaths = {
            "cadastradaPor",
            "itens",
            "itens.material"
    })
    @Query("""
            SELECT DISTINCT nota
            FROM NotaFiscalEntrada nota
            WHERE nota.id = :id
            """)
    Optional<NotaFiscalEntrada> findDetalhadaById(@Param("id") Long id);

    @EntityGraph(attributePaths = {
            "cadastradaPor",
            "itens",
            "itens.material"
    })
    @Query("""
            SELECT DISTINCT nota
            FROM NotaFiscalEntrada nota
            ORDER BY nota.dataCadastro DESC, nota.id DESC
            """)
    List<NotaFiscalEntrada> findAllDetalhadas();
}
