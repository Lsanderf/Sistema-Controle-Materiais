package com.Lucca.Projeto1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class MaterialRequest {
    @NotBlank(message = "O nome e obrigatorio")
    private String nome;

    private String descricao;

    @NotNull(message = "A quantidade em estoque e obrigatoria")
    @PositiveOrZero(message = "A quantidade em estoque nao pode ser negativa")
    private Integer quantidadeEstoque;

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public Integer getQuantidadeEstoque() {
        return quantidadeEstoque;
    }
}
