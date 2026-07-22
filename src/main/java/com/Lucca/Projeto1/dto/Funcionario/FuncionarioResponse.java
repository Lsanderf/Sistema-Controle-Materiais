package com.Lucca.Projeto1.dto.Funcionario;

public class FuncionarioResponse {
    private Long id;
    private String nome;
    private String cargo;

    public FuncionarioResponse(
            Long id,
            String nome,
            String cargo
    ) {
        this.id = id;
        this.nome = nome;
        this.cargo = cargo;
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
}
