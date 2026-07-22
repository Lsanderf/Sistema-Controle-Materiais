package com.Lucca.Projeto1.dto.AtualizacaoMaterial;


import jakarta.validation.constraints.NotBlank;

public class MaterialAtualizacaoRequest {

    @NotBlank(message = "O nome do material é obrigatório")
    private String nome;

    @NotBlank(message = "A descrição do material é obrigatória")
    private String descricao;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}