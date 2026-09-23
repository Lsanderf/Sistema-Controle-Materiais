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

    static String proximoCpf() {
        String base = String.format("%09d", CPF_SEQUENCE.getAndIncrement());
        int primeiroDigito = calcularDigito(base, 10);
        int segundoDigito = calcularDigito(base + primeiroDigito, 11);
        return base + primeiroDigito + segundoDigito;
    }

    private static int calcularDigito(String parcial, int pesoInicial) {
        int soma = 0;
        for (int indice = 0; indice < parcial.length(); indice++) {
            soma += Character.digit(parcial.charAt(indice), 10)
                    * (pesoInicial - indice);
        }

        int resultado = 11 - (soma % 11);
        return resultado >= 10 ? 0 : resultado;
    }
}
