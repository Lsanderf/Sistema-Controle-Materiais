package com.Lucca.Projeto1.dto.movimentacao;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class EntradaEstoqueRequest {

    @NotNull(message = "O ID do material é obrigatório")
    @Positive(message = "O material deve ser maior que zero")
    private Long materialId;

    @NotNull(message = "A quantidade é obrigatória")
    @Positive(message = "A quantidade deve ser maior que zero")
    @Max(value = 10000, message = "A quantidade máxima por operação é 10.000")
    private Integer quantidade;

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
}
