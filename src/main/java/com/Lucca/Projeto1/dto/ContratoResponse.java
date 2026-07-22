package com.Lucca.Projeto1.dto;

public class ContratoResponse {
    private Long id;
    private String nome;
    private String descricao;
    private Boolean ativo;

    public ContratoResponse(
            Long id,
            String nome,
            String descricao,
            Boolean ativo
    ) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.ativo = ativo;
    }

    public Long getId() {
        return id;
    }

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
