package com.Lucca.Projeto1.dto.Funcionario;

import jakarta.validation.constraints.NotBlank;

public class FuncionarioRequest {
    @NotBlank(message = "O nome é obrigatório")
    private String nome;

    @NotBlank(message = "O CPF é obrigatório")
    private String cpf;

    @NotBlank(message = "O cargo é obrigatório")
    private String cargo;

    public String getNome() {
        return nome;
    }

    public String getCpf() {
        return cpf;
    }

    public String getCargo() {
        return cargo;
    }
}
