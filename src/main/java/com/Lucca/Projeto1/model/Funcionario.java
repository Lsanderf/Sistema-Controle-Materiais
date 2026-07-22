package com.Lucca.Projeto1.model;


import jakarta.persistence.*;

@Entity
@Table(name = "tb_funcionarios")
public class Funcionario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String cpf;

    @Column(nullable = false)
    private String cargo;

    @Column(nullable = false)
    private Boolean ativo;

    public Funcionario(){}

    public Funcionario(String nome, String cpf, String cargo){
        this.nome = nome;
        this.cpf = cpf;
        this.cargo = cargo;
        ativo = true;
    }

    public long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public Boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

}
