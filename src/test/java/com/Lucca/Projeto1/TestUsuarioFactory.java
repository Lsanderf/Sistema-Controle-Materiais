package com.Lucca.Projeto1;

import com.Lucca.Projeto1.model.Role;
import com.Lucca.Projeto1.model.Usuario;
import com.Lucca.Projeto1.service.UsuarioService;

import java.util.concurrent.atomic.AtomicLong;

final class TestUsuarioFactory {

    private static final AtomicLong CPF_SEQUENCE = new AtomicLong(100_000_000L);

    private TestUsuarioFactory() {
    }

    static Usuario criarUsuario(
            UsuarioService service,
            String username,
            String password,
            Role role,
            boolean ativo
    ) {
        return service.criarUsuario(
                "Usuário " + username,
                proximoCpf(),
                "11999999999",
                username,
                password,
                role,
                ativo
        );
    }

    static Usuario criarUsuario(
            UsuarioService service,
            String nome,
            String cpf,
            String celular,
            String username,
            String password,
            Role role,
            boolean ativo
    ) {
        return service.criarUsuario(
                nome,
                cpf,
                celular,
                username,
                password,
                role,
                ativo
        );
    }

    static Usuario encarregado(String nome, String cpf, String cargoLegadoIgnorado) {
        String cpfNormalizado = cpf.replaceAll("[^0-9]", "");
        return new Usuario(
                nome,
                cpfNormalizado,
                "11999999999",
                "encarregado-" + cpfNormalizado,
                "senha-nao-utilizada",
                Role.ENCARREGADO,
                true
        );
    }

    static Usuario usuarioPersistivel(
            String username,
            String senha,
            Role role,
            boolean ativo
    ) {
        return new Usuario(
                "Usuário " + username,
                proximoCpf(),
                "11999999999",
                username,
                senha,
                role,
                ativo
        );
    }

    private static String proximoCpf() {
        String base = String.format("%09d", CPF_SEQUENCE.getAndIncrement());
        int primeiro = digitoCpf(base, 10);
        int segundo = digitoCpf(base + primeiro, 11);
        return base + primeiro + segundo;
    }

    private static int digitoCpf(String parcial, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < parcial.length(); i++) {
            soma += Character.digit(parcial.charAt(i), 10) * (pesoInicial - i);
        }
        int resto = 11 - (soma % 11);
        return resto >= 10 ? 0 : resto;
    }
}
