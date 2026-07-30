package com.Lucca.Projeto1.dto.auth;

import com.Lucca.Projeto1.model.Role;

public class LoginResponse {

    private String token;
    private String tipo;
    private Long expiraEm;
    private Role role;

    public LoginResponse(String token, String tipo, Long expiraEm, Role role) {
        this.token = token;
        this.tipo = tipo;
        this.expiraEm = expiraEm;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public String getTipo() {
        return tipo;
    }

    public Long getExpiraEm() {
        return expiraEm;
    }

    public Role getRole() {
        return role;
    }
}
