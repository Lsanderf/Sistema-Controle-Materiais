package com.Lucca.Projeto1.repository;

import com.Lucca.Projeto1.model.Contrato;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;

public interface ContratoRepository extends JpaRepository<Contrato, Long> {
    Optional<Contrato> findByNomeIgnoreCase(String nome);
}
