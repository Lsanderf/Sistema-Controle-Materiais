package com.Lucca.Projeto1.repository;

import com.Lucca.Projeto1.model.AssinaturaRetirada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssinaturaRetiradaRepository extends JpaRepository<AssinaturaRetirada, Long> {
    boolean existsBySolicitacaoRetiradaId(Long solicitacaoId);
    Optional<AssinaturaRetirada> findBySolicitacaoRetiradaId(Long solicitacaoId);
}
