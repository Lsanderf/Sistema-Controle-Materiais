package com.Lucca.Projeto1.dto.usuario;

import com.Lucca.Projeto1.model.Role;

import java.time.LocalDateTime;

public class UsuarioResponse {

    private Long id;
    private String nome;
    private String celular;
    private String username;
    private Role role;
    private Boolean ativo;
    private LocalDateTime dataInativacao;

    public UsuarioResponse(
            Long id,
            String nome,
            String celular,
            String username,
            Role role,
            Boolean ativo,
            LocalDateTime dataInativacao
    ) {
        this.id = id;
        this.nome = nome;
        this.celular = celular;
        this.username = username;
        this.role = role;
        this.ativo = ativo;
        this.dataInativacao = dataInativacao;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCelular() {
        return celular;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public LocalDateTime getDataInativacao() {
        return dataInativacao;
    }
}
