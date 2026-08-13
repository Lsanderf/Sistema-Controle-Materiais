package com.Lucca.Projeto1.dto.contrato;

import java.time.LocalDateTime;

public class ContratoResponse {
    private Long id;
    private String nome;
    private String descricao;
    private Boolean ativo;
    private LocalDateTime dataInativacao;

    public ContratoResponse(
            Long id,
            String nome,
            String descricao,
            Boolean ativo,
            LocalDateTime dataInativacao
    ) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.ativo = ativo;
        this.dataInativacao = dataInativacao;
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

    public LocalDateTime getDataInativacao() {
        return dataInativacao;
    }
}
