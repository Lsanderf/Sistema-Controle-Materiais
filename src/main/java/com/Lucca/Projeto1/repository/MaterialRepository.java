package com.Lucca.Projeto1.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Lucca.Projeto1.model.Material;

public interface    MaterialRepository extends JpaRepository<Material, Long>{
    Optional<Material> findByNomeIgnoreCase(String nome);
}
