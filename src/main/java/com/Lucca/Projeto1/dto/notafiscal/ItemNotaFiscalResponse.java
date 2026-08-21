package com.Lucca.Projeto1.dto.notafiscal;

import java.math.BigDecimal;

public class ItemNotaFiscalResponse {

    private final Long id;
    private final Long materialId;
    private final String material;
    private final Integer quantidade;
    private final BigDecimal valorUnitario;
    private final BigDecimal valorTotal;

    public ItemNotaFiscalResponse(
            Long id,
            Long materialId,
            String material,
            Integer quantidade,
            BigDecimal valorUnitario,
            BigDecimal valorTotal
    ) {
        this.id = id;
        this.materialId = materialId;
        this.material = material;
        this.quantidade = quantidade;
        this.valorUnitario = valorUnitario;
        this.valorTotal = valorTotal;
    }

    public Long getId() {
        return id;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public String getMaterial() {
        return material;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public BigDecimal getValorUnitario() {
        return valorUnitario;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }
}
