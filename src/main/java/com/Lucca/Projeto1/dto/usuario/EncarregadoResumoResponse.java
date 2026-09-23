package com.Lucca.Projeto1.dto.usuario;

public record EncarregadoResumoResponse(
        Long id,
        String nome,
        String celular,
        String username,
        Boolean ativo
) {
}
