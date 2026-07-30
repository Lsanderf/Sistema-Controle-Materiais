package com.Lucca.Projeto1.service;

import com.Lucca.Projeto1.dto.usuario.UsuarioRequest;
import com.Lucca.Projeto1.dto.usuario.UsuarioResponse;
import com.Lucca.Projeto1.exception.RegraNegocioException;
import com.Lucca.Projeto1.model.Role;
import com.Lucca.Projeto1.model.Usuario;
import com.Lucca.Projeto1.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioResponse cadastrar(UsuarioRequest request) {
        Usuario usuario = criarUsuario(
                request.getUsername(),
                request.getPassword(),
                request.getRole(),
                true
        );

        return paraResponse(usuario);
    }

    @Transactional
    public Usuario criarUsuario(
            String username,
            String password,
            Role role,
            boolean ativo
    ) {
        String usernameNormalizado = normalizarUsername(username);

        if (usuarioRepository.existsByUsernameIgnoreCase(usernameNormalizado)) {
            throw new RegraNegocioException(
                    "Já existe um usuário com esse nome"
            );
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(usernameNormalizado);
        usuario.setSenha(passwordEncoder.encode(password));
        usuario.setRole(role);
        usuario.setAtivo(ativo);

        return usuarioRepository.save(usuario);
    }

    public UsuarioResponse paraResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getRole(),
                usuario.getAtivo()
        );
    }

    private String normalizarUsername(String username) {
        return username == null ? null : username.trim();
    }
}
