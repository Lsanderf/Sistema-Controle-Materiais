package com.Lucca.Projeto1.repository;

import com.Lucca.Projeto1.model.Role;
import com.Lucca.Projeto1.model.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT u
            FROM Usuario u
            WHERE u.role = :role
              AND u.ativo = true
            """)
    List<Usuario> buscarAtivosPorRoleComBloqueio(Role role);
}
