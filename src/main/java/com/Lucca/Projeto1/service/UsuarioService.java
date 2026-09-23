package com.Lucca.Projeto1.service;

import com.Lucca.Projeto1.dto.usuario.CriarEncarregadoRequest;
import com.Lucca.Projeto1.dto.usuario.CriarUsuarioRequest;
import com.Lucca.Projeto1.dto.usuario.EncarregadoResumoResponse;
import com.Lucca.Projeto1.dto.usuario.UsuarioAtualizacaoRequest;
import com.Lucca.Projeto1.dto.usuario.UsuarioResponse;
import com.Lucca.Projeto1.exception.RecursoNaoEncontradoException;
import com.Lucca.Projeto1.exception.RegraNegocioException;
import com.Lucca.Projeto1.model.Role;
import com.Lucca.Projeto1.model.Usuario;
import com.Lucca.Projeto1.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
    public UsuarioResponse cadastrar(CriarUsuarioRequest request) {
        Usuario usuario = criarUsuario(
                request.getNome(),
                request.getCpf(),
                request.getCelular(),
                request.getUsername(),
                request.getPassword(),
                request.getRole(),
                true
        );

        return paraResponse(usuario);
    }

    public List<UsuarioResponse> listarTodos() {
        return usuarioRepository.findAll()
                .stream()
                .map(this::paraResponse)
                .toList();
    }

    public List<EncarregadoResumoResponse> listarEncarregados() {
        return usuarioRepository
                .findByRoleOrderByNomeAsc(Role.ENCARREGADO)
                .stream()
                .map(this::paraEncarregadoResumoResponse)
                .toList();
    }

    @Transactional
    public EncarregadoResumoResponse cadastrarEncarregado(
            CriarEncarregadoRequest request
    ) {
        Usuario usuario = criarUsuario(
                request.getNome(),
                request.getCpf(),
                request.getCelular(),
                request.getUsername(),
                request.getPassword(),
                Role.ENCARREGADO,
                true
        );

        return paraEncarregadoResumoResponse(usuario);
    }

    public UsuarioResponse buscarPorId(Long id) {
        return paraResponse(buscarEntidade(id));
    }

    @Transactional
    public UsuarioResponse atualizar(
            Long id,
            UsuarioAtualizacaoRequest request
    ) {
        Usuario usuario = buscarEntidade(id);
        String usernameNormalizado =
                normalizarUsername(request.getUsername());

        usuarioRepository
                .findByUsernameIgnoreCase(usernameNormalizado)
                .filter(outroUsuario ->
                        !outroUsuario.getId().equals(usuario.getId())
                )
                .ifPresent(outroUsuario -> {
                    throw new RegraNegocioException(
                            "Já existe um usuário com esse username"
                    );
                });

        if (usuario.isAtivo()
                && usuario.getRole() == Role.ADMIN
                && request.getRole() != Role.ADMIN) {
            validarQueNaoEhUltimoAdminAtivo();
        }

        usuario.setUsername(usernameNormalizado);
        usuario.setRole(request.getRole());

        if (request.getNovaSenha() != null
                && !request.getNovaSenha().isBlank()) {
            usuario.setSenha(
                    passwordEncoder.encode(request.getNovaSenha())
            );
        }

        return paraResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse ativar(Long id) {
        Usuario usuario = buscarEntidade(id);

        if (usuario.isAtivo()) {
            throw new RegraNegocioException(
                    "O usuário já está ativo"
            );
        }

        usuario.setAtivo(true);
        usuario.setDataInativacao(null);
        return paraResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse desativar(
            Long id,
            String usernameAutenticado
    ) {
        Usuario usuario = buscarEntidade(id);

        if (usuario.getUsername().equalsIgnoreCase(usernameAutenticado)) {
            throw new RegraNegocioException(
                    "Você não pode desativar sua própria conta"
            );
        }

        if (!usuario.isAtivo()) {
            throw new RegraNegocioException(
                    "O usuário já está inativo"
            );
        }

        if (usuario.getRole() == Role.ADMIN) {
            validarQueNaoEhUltimoAdminAtivo();
        }

        usuario.setAtivo(false);
        usuario.setDataInativacao(LocalDateTime.now());
        return paraResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public Usuario criarUsuario(
            String nome,
            String cpf,
            String celular,
            String username,
            String password,
            Role role,
            boolean ativo
    ) {
        String usernameNormalizado = normalizarUsername(username);
        String cpfNormalizado = normalizarCpf(cpf);

        if (usuarioRepository.existsByUsernameIgnoreCase(usernameNormalizado)) {
            throw new RegraNegocioException(
                    "Já existe um usuário com esse username"
            );
        }

        if (usuarioRepository.existsByCpf(cpfNormalizado)) {
            throw new RegraNegocioException(
                    "Já existe um usuário com esse CPF"
            );
        }

        Usuario usuario = new Usuario();
        usuario.setNome(normalizarTexto(nome));
        usuario.setCpf(cpfNormalizado);
        usuario.setCelular(normalizarCelular(celular));
        usuario.setUsername(usernameNormalizado);
        usuario.setSenha(passwordEncoder.encode(password));
        usuario.setRole(role);
        usuario.setAtivo(ativo);

        return usuarioRepository.save(usuario);
    }

    public UsuarioResponse paraResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getCelular(),
                usuario.getUsername(),
                usuario.getRole(),
                usuario.getAtivo(),
                usuario.getDataInativacao()
        );
    }

    private EncarregadoResumoResponse paraEncarregadoResumoResponse(
            Usuario usuario
    ) {
        return new EncarregadoResumoResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getCelular(),
                usuario.getUsername(),
                usuario.getAtivo()
        );
    }

    private Usuario buscarEntidade(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Usuário não encontrado"
                        )
                );
    }

    private String normalizarUsername(String username) {
        return username == null ? null : username.trim();
    }

    private String normalizarCpf(String cpf) {
        return cpf == null ? null : cpf.replaceAll("[^0-9]", "");
    }

    private String normalizarCelular(String celular) {
        return celular == null ? null : celular.replaceAll("[^0-9]", "");
    }

    private String normalizarTexto(String texto) {
        return texto == null ? null : texto.trim();
    }

    private void validarQueNaoEhUltimoAdminAtivo() {
        if (usuarioRepository
                .buscarAtivosPorRoleComBloqueio(Role.ADMIN)
                .size() <= 1) {
            throw new RegraNegocioException(
                    "O último administrador ativo não pode ser desativado "
                            + "nem perder o perfil ADMIN"
            );
        }
    }
}
