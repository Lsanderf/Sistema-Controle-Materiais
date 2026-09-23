package com.Lucca.Projeto1.config;

import com.Lucca.Projeto1.model.Role;
import com.Lucca.Projeto1.repository.UsuarioRepository;
import com.Lucca.Projeto1.service.UsuarioService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class UsuarioAdminInitializer implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final Environment environment;

    public UsuarioAdminInitializer(
            UsuarioRepository usuarioRepository,
            UsuarioService usuarioService,
            Environment environment
    ) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.count() > 0) {
            return;
        }

        String username = environment.getProperty("APP_ADMIN_USERNAME");
        String password = environment.getProperty("APP_ADMIN_PASSWORD");
        String nome = environment.getProperty("APP_ADMIN_NOME", "Administrador");
        String cpf = environment.getProperty("APP_ADMIN_CPF", "52998224725");
        String celular = environment.getProperty(
                "APP_ADMIN_CELULAR",
                "11999999999"
        );

        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
            return;
        }

        usuarioService.criarUsuario(
                nome,
                cpf,
                celular,
                username,
                password,
                Role.ADMIN,
                true
        );
    }
}
