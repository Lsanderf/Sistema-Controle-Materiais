package com.Lucca.Projeto1.dto;

public class MaterialResponse {
    private Long id;
    private String nome;
    private String descricao;
    private Integer quantidadeEstoque;

    public MaterialResponse(
            Long id,
            String nome,
            String descricao,
            Integer quantidadeEstoque
    ) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.quantidadeEstoque = quantidadeEstoque;
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

    public Integer getQuantidadeEstoque() {
        return quantidadeEstoque;
    }
}
