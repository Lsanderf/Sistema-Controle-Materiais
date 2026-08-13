package com.Lucca.Projeto1.security;

import com.Lucca.Projeto1.model.Usuario;
import com.Lucca.Projeto1.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class UsuarioAtivoFilter extends OncePerRequestFilter {

    private final UsuarioRepository usuarioRepository;
    private final ApiSecurityErrorWriter errorWriter;

    public UsuarioAtivoFilter(
            UsuarioRepository usuarioRepository,
            ApiSecurityErrorWriter errorWriter
    ) {
        this.usuarioRepository = usuarioRepository;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        Usuario usuario = usuarioRepository
                .findByUsernameIgnoreCase(authentication.getName())
                .orElse(null);

        if (usuario == null || !usuario.isAtivo()) {
            rejeitar(
                    response,
                    "Usuário inativo ou não encontrado. Entre novamente"
            );
            return;
        }

        String autoridadeEsperada = "ROLE_" + usuario.getRole().name();
        boolean roleAtual = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals(autoridadeEsperada)
                );

        if (!roleAtual) {
            rejeitar(
                    response,
                    "As permissões do usuário foram alteradas. Entre novamente"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void rejeitar(
            HttpServletResponse response,
            String mensagem
    ) throws IOException {
        SecurityContextHolder.clearContext();
        errorWriter.write(response, HttpStatus.UNAUTHORIZED, mensagem);
    }
}
