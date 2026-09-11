package com.Lucca.Projeto1.dto.movimentacao;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.Lucca.Projeto1.model.TipoMovimentacao;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class MovimentacaoRequest {

    @NotNull(message = "O funcionário é obrigatório")
    @Positive(message = "O funcionário deve ser maior que zero")
    private Long funcionarioId;

    @NotNull(message = "O contrato é obrigatório")
    @Positive(message = "O contrato deve ser maior que zero")
    private Long contratoId;

    @NotNull(message = "O material é obrigatório")
    @Positive(message = "O material deve ser maior que zero")
    private Long materialId;

    @NotNull(message = "A quantidade é obrigatória")
    @Positive(message = "A quantidade deve ser maior que zero")
    @Max(value = 10000, message = "A quantidade máxima por operação é 10.000")
    private Integer quantidade;

    @NotNull(message = "O tipo da movimentação é obrigatório")
    private TipoMovimentacao tipo;

    @Size(max = 1000, message = "A observação deve possuir no máximo 1.000 caracteres")
    private String observacao;

    public Long getFuncionarioId() {
        return funcionarioId;
    }

    public void setFuncionarioId(Long funcionarioId) {
        this.funcionarioId = funcionarioId;
    }

    public Long getContratoId() {
        return contratoId;
    }

    public void setContratoId(Long contratoId) {
        this.contratoId = contratoId;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public TipoMovimentacao getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimentacao tipo) {
        this.tipo = tipo;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    @JsonAnySetter
    public void rejeitarCampoDesconhecido(String campo, Object valor) {
        throw new IllegalArgumentException("Campo não permitido: " + campo);
    }
}
