package com.Lucca.Projeto1.repository;

import com.Lucca.Projeto1.model.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {
}
