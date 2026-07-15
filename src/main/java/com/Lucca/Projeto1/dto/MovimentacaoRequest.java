package com.Lucca.Projeto1.dto;

import com.Lucca.Projeto1.model.TipoMovimentacao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class MovimentacaoRequest {
    @NotNull(message = "O funcionário é obrigatório")
    private Long funcionarioId;

    @NotNull(message = "O contrato é obrigatório")
    private Long contratoId;

    @NotNull(message = "O material é obrigatório")
    private Long materialId;

    @NotNull(message = "A quantidade é obrigatória")
    @Positive(message = "A quantidade deve ser maior que zero")
    private Integer quantidade;

    @NotNull(message = "O tipo da movimentação é obrigatório")
    private TipoMovimentacao tipo;


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
}

