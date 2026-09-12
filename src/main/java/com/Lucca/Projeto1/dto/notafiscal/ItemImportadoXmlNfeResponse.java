package com.Lucca.Projeto1.dto.notafiscal;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;

public record ItemImportadoXmlNfeResponse(
        Integer numeroItem,
        String codigoProduto,
        String descricaoProduto,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal quantidadeComercial,
        String unidadeComercial,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal valorUnitarioComercial,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal valorTotal,
        String eanGtin
) {
}
