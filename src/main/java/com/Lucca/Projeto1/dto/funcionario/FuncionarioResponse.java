package com.Lucca.Projeto1.dto.funcionario;

import java.time.LocalDateTime;

public class FuncionarioResponse {

    private Long id;
    private String nome;
    private String cargo;
    private boolean ativo;
    private LocalDateTime dataInativacao;

    public FuncionarioResponse(
            Long id,
            String nome,
            String cargo,
            boolean ativo,
            LocalDateTime dataInativacao
    ) {
        this.id = id;
        this.nome = nome;
        this.cargo = cargo;
        this.ativo = ativo;
        this.dataInativacao = dataInativacao;
    }


    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCargo() {
        return cargo;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public LocalDateTime getDataInativacao() {
        return dataInativacao;
    }
}
