package com.Lucca.Projeto1.dto.notafiscal;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class ItemNotaFiscalRequest {

    @NotNull(message = "O material é obrigatório")
    @Positive(message = "O material deve ser maior que zero")
    private Long materialId;

    @NotNull(message = "A quantidade é obrigatória")
    @Positive(message = "A quantidade deve ser maior que zero")
    @Max(value = 10000, message = "A quantidade máxima por item é 10.000")
    private Integer quantidade;

    @NotNull(message = "O valor unitário é obrigatório")
    @DecimalMin(value = "0.00", inclusive = true,
            message = "O valor unitário não pode ser negativo")
    @Digits(integer = 17, fraction = 2,
            message = "O valor unitário deve possuir no máximo 17 inteiros e 2 decimais")
    private BigDecimal valorUnitario;

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

    public BigDecimal getValorUnitario() {
        return valorUnitario;
    }

    public void setValorUnitario(BigDecimal valorUnitario) {
        this.valorUnitario = valorUnitario;
    }

    @JsonAnySetter
    public void rejeitarCampoDesconhecido(String campo, Object valor) {
        throw new IllegalArgumentException("Campo não permitido: " + campo);
    }
}
