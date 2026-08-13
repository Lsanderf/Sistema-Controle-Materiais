package com.Lucca.Projeto1.dto.usuario;

import com.Lucca.Projeto1.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UsuarioAtualizacaoRequest {

    @NotBlank(message = "O nome de usuário é obrigatório")
    @Size(
            min = 3,
            max = 100,
            message = "O nome de usuário deve ter entre 3 e 100 caracteres"
    )
    private String username;

    @NotNull(message = "O papel do usuário é obrigatório")
    private Role role;

    @Pattern(
            regexp = "(?s)^(?:\\s*|.{8,100})$",
            message = "A nova senha deve ter entre 8 e 100 caracteres"
    )
    private String novaSenha;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getNovaSenha() {
        return novaSenha;
    }

    public void setNovaSenha(String novaSenha) {
        this.novaSenha = novaSenha;
    }
}
