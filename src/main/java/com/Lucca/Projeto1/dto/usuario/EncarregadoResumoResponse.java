package com.Lucca.Projeto1.dto.usuario;

/**
 * Dados mínimos para seleção e exibição operacional de encarregados.
 * Dados pessoais sensíveis, como CPF, permanecem nos endpoints administrativos.
 */
public record EncarregadoResumoResponse(
        Long id,
        String nome,
        String celular,
        String username,
        Boolean ativo
) {
}
