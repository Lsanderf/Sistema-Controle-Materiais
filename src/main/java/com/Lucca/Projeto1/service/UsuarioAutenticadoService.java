package com.Lucca.Projeto1.service;

import com.Lucca.Projeto1.exception.RegraNegocioException;
import com.Lucca.Projeto1.model.Usuario;
import com.Lucca.Projeto1.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioAutenticadoService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioAutenticadoService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario obter() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RegraNegocioException(
                    "Usuário autenticado não identificado"
            );
        }

        Usuario usuario = usuarioRepository
                .findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() ->
                        new RegraNegocioException(
                                "Usuário autenticado não encontrado"
                        )
                );

        if (!usuario.isAtivo()) {
            throw new RegraNegocioException("Usuário inativo");
        }

        return usuario;
    }
}
