package com.Lucca.Projeto1.repository;

import com.Lucca.Projeto1.model.Requisicao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RequisicaoRepository extends JpaRepository<Requisicao, Long> {

    @Query("""
            select distinct r from Requisicao r
            left join fetch r.gerenteSolicitante
            left join fetch r.encarregadoDestinatario
            left join fetch r.operadorRegistrador
            left join fetch r.encarregadoNecessidade
            left join fetch r.gerenteDestinatario
            join fetch r.contrato
            join fetch r.itens
            order by r.criadaEm desc
            """)
    List<Requisicao> findAllComDetalhes();

    @Query("""
            select distinct r from Requisicao r
            left join fetch r.gerenteSolicitante
            left join fetch r.encarregadoDestinatario
            left join fetch r.operadorRegistrador
            left join fetch r.encarregadoNecessidade
            left join fetch r.gerenteDestinatario
            join fetch r.contrato
            join fetch r.itens
            where r.gerenteSolicitante.id = :usuarioId or r.gerenteDestinatario.id = :usuarioId
            order by r.criadaEm desc
            """)
    List<Requisicao> findByGerenteSolicitanteIdComDetalhes(@Param("usuarioId") Long usuarioId);

    @Query("""
            select distinct r from Requisicao r
            left join fetch r.gerenteSolicitante
            left join fetch r.encarregadoDestinatario
            left join fetch r.operadorRegistrador
            left join fetch r.encarregadoNecessidade
            left join fetch r.gerenteDestinatario
            join fetch r.contrato
            join fetch r.itens
            where r.encarregadoDestinatario.id = :usuarioId or r.encarregadoNecessidade.id = :usuarioId
            order by r.criadaEm desc
            """)
    List<Requisicao> findByEncarregadoDestinatarioIdComDetalhes(@Param("usuarioId") Long usuarioId);

    @Query("""
            select distinct r from Requisicao r
            left join fetch r.gerenteSolicitante
            left join fetch r.encarregadoDestinatario
            left join fetch r.operadorRegistrador
            left join fetch r.encarregadoNecessidade
            left join fetch r.gerenteDestinatario
            join fetch r.contrato
            join fetch r.itens
            where r.id = :id
            """)
    Optional<Requisicao> findByIdComDetalhes(@Param("id") Long id);
}
