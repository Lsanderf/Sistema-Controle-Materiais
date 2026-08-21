package com.Lucca.Projeto1.dto.notafiscal;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CNPJ;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class NotaFiscalRequest {

    @NotBlank(message = "O número da nota fiscal é obrigatório")
    @Size(max = 50, message = "O número deve possuir no máximo 50 caracteres")
    private String numero;

    @NotBlank(message = "A série da nota fiscal é obrigatória")
    @Size(max = 20, message = "A série deve possuir no máximo 20 caracteres")
    private String serie;

    @NotBlank(message = "A chave de acesso é obrigatória")
    @Size(max = 80, message = "A chave de acesso formatada é muito longa")
    @Pattern(regexp = "[0-9.\\-/\\s]+",
            message = "A chave de acesso deve conter apenas números e formatação")
    private String chaveAcesso;

    @NotBlank(message = "O fornecedor é obrigatório")
    @Size(max = 200, message = "O fornecedor deve possuir no máximo 200 caracteres")
    private String fornecedor;

    @NotBlank(message = "O CNPJ do fornecedor é obrigatório")
    @CNPJ(message = "O CNPJ do fornecedor é inválido")
    private String cnpjFornecedor;

    @NotNull(message = "A data de emissão é obrigatória")
    @PastOrPresent(message = "A data de emissão não pode estar no futuro")
    private LocalDate dataEmissao;

    @NotNull(message = "A lista de itens é obrigatória, mas pode estar vazia")
    @Valid
    private List<ItemNotaFiscalRequest> itens = new ArrayList<>();

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie;
    }

    public String getChaveAcesso() {
        return chaveAcesso;
    }

    public void setChaveAcesso(String chaveAcesso) {
        this.chaveAcesso = chaveAcesso;
    }

    public String getFornecedor() {
        return fornecedor;
    }

    public void setFornecedor(String fornecedor) {
        this.fornecedor = fornecedor;
    }

    public String getCnpjFornecedor() {
        return cnpjFornecedor;
    }

    public void setCnpjFornecedor(String cnpjFornecedor) {
        this.cnpjFornecedor = cnpjFornecedor;
    }

    public LocalDate getDataEmissao() {
        return dataEmissao;
    }

    public void setDataEmissao(LocalDate dataEmissao) {
        this.dataEmissao = dataEmissao;
    }

    public List<ItemNotaFiscalRequest> getItens() {
        return itens;
    }

    public void setItens(List<ItemNotaFiscalRequest> itens) {
        this.itens = itens;
    }

    @JsonAnySetter
    public void rejeitarCampoDesconhecido(String campo, Object valor) {
        throw new IllegalArgumentException("Campo não permitido: " + campo);
    }
}
