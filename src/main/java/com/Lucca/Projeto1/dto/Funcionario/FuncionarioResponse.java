package com.Lucca.Projeto1.dto.Funcionario;

public class FuncionarioResponse {

    private Long id;
    private String nome;
    private String cargo;
    private boolean ativo;

    public FuncionarioResponse(
            Long id,
            String nome,
            String cargo,
            boolean ativo
    ) {
        this.id = id;
        this.nome = nome;
        this.cargo = cargo;
        this.ativo = ativo;
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
}