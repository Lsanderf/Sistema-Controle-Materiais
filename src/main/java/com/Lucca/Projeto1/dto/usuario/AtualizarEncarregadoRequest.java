package com.Lucca.Projeto1.dto.usuario;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties("role")
public class AtualizarEncarregadoRequest {

    @NotBlank(message = "O nome de usuário é obrigatório")
    @Size(min = 3, max = 100, message = "O nome de usuário deve ter entre 3 e 100 caracteres")
    private String username;

    @Pattern(
            regexp = "(?s)^(?:\\s*|.{8,100})$",
            message = "A nova senha deve ter entre 8 e 100 caracteres"
    )
    private String novaSenha;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNovaSenha() { return novaSenha; }
    public void setNovaSenha(String novaSenha) { this.novaSenha = novaSenha; }
}
