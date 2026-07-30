package com.Lucca.Projeto1.service;

import com.Lucca.Projeto1.dto.auth.LoginRequest;
import com.Lucca.Projeto1.dto.auth.LoginResponse;
import com.Lucca.Projeto1.exception.CredenciaisInvalidasException;
import com.Lucca.Projeto1.model.Usuario;
import com.Lucca.Projeto1.repository.UsuarioRepository;
import com.Lucca.Projeto1.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository
                .findByUsernameIgnoreCase(request.getUsername())
                .filter(Usuario::isAtivo)
                .orElseThrow(this::credenciaisInvalidas);

        if (!passwordEncoder.matches(request.getPassword(), usuario.getSenha())) {
            throw credenciaisInvalidas();
        }

        return new LoginResponse(
                jwtService.gerarToken(usuario),
                "Bearer",
                jwtService.getExpirationSeconds(),
                usuario.getRole()
        );
    }

    private CredenciaisInvalidasException credenciaisInvalidas() {
        return new CredenciaisInvalidasException(
                "Usuário ou senha inválidos"
        );
    }
}
