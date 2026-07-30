package com.Lucca.Projeto1.repository;

import com.Lucca.Projeto1.model.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {
    Optional<Funcionario> findByCpf(String cpf);

    @Query("""
            SELECT funcionario
            FROM Funcionario funcionario
            WHERE REPLACE(REPLACE(REPLACE(funcionario.cpf, '.', ''), '-', ''), ' ', '') = :cpf
            """)
    Optional<Funcionario> findByCpfNormalizado(@Param("cpf") String cpf);
}
