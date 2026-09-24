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
            join fetch r.gerenteSolicitante
            join fetch r.encarregadoDestinatario
            join fetch r.contrato
            join fetch r.itens i
            join fetch i.material
            order by r.criadaEm desc
            """)
    List<Requisicao> findAllComDetalhes();

    @Query("""
            select distinct r from Requisicao r
            join fetch r.gerenteSolicitante
            join fetch r.encarregadoDestinatario
            join fetch r.contrato
            join fetch r.itens i
            join fetch i.material
            where r.gerenteSolicitante.id = :usuarioId
            order by r.criadaEm desc
            """)
    List<Requisicao> findByGerenteSolicitanteIdComDetalhes(@Param("usuarioId") Long usuarioId);

    @Query("""
            select distinct r from Requisicao r
            join fetch r.gerenteSolicitante
            join fetch r.encarregadoDestinatario
            join fetch r.contrato
            join fetch r.itens i
            join fetch i.material
            where r.encarregadoDestinatario.id = :usuarioId
            order by r.criadaEm desc
            """)
    List<Requisicao> findByEncarregadoDestinatarioIdComDetalhes(@Param("usuarioId") Long usuarioId);

    @Query("""
            select distinct r from Requisicao r
            join fetch r.gerenteSolicitante
            join fetch r.encarregadoDestinatario
            join fetch r.contrato
            join fetch r.itens i
            join fetch i.material
            where r.id = :id
            """)
    Optional<Requisicao> findByIdComDetalhes(@Param("id") Long id);
}
