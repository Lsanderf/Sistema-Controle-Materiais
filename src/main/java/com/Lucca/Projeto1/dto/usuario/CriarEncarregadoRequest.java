package com.Lucca.Projeto1.dto.usuario;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

@JsonIgnoreProperties("role")
public class CriarEncarregadoRequest {

    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 2, max = 150, message = "O nome deve ter entre 2 e 150 caracteres")
    private String nome;

    @NotBlank(message = "O CPF é obrigatório")
    @CPF(message = "O CPF informado é inválido")
    private String cpf;

    @NotBlank(message = "O celular é obrigatório")
    @Pattern(
            regexp = "^[0-9() +.-]{8,20}$",
            message = "O celular informado é inválido"
    )
    private String celular;

    @NotBlank(message = "O nome de usuário é obrigatório")
    @Size(min = 3, max = 100, message = "O nome de usuário deve ter entre 3 e 100 caracteres")
    private String username;

    @NotBlank(message = "A senha é obrigatória")
    @Size(min = 8, max = 100, message = "A senha deve ter entre 8 e 100 caracteres")
    private String password;

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

    public String getCelular() {
        return celular;
    }

    public void setCelular(String celular) {
        this.celular = celular;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
