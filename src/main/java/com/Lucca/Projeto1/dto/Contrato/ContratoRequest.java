package com.Lucca.Projeto1.dto.Contrato;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ContratoRequest {
    @NotBlank(message = "O nome e obrigatorio")
    private String nome;

    private String descricao;

    @NotNull(message = "O status ativo e obrigatorio")
    private Boolean ativo;

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public Boolean getAtivo() {
        return ativo;
    }
}
