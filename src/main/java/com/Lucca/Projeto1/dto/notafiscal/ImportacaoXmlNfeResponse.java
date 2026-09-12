package com.Lucca.Projeto1.dto.notafiscal;

import java.time.LocalDate;
import java.util.List;

public record ImportacaoXmlNfeResponse(
        String chaveAcesso,
        String numero,
        String serie,
        LocalDate dataEmissao,
        String cnpjFornecedor,
        String fornecedor,
        List<ItemImportadoXmlNfeResponse> itens
) {
}
