package com.Lucca.Projeto1.dto.usuario;

import com.Lucca.Projeto1.model.Role;

public class UsuarioResponse {

    private Long id;
    private String username;
    private Role role;
    private Boolean ativo;

    public UsuarioResponse(Long id, String username, Role role, Boolean ativo) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.ativo = ativo;
    }

    public Long getId() {
        return id;
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
}
