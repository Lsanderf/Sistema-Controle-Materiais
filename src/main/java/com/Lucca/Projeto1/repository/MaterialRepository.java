package com.Lucca.Projeto1.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.Lucca.Projeto1.model.Material;

public interface    MaterialRepository extends JpaRepository<Material, Long>{
    Optional<Material> findByNomeIgnoreCase(String nome);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT material FROM Material material WHERE material.id = :id")
    Optional<Material> findByIdComBloqueio(@Param("id") Long id);
}
