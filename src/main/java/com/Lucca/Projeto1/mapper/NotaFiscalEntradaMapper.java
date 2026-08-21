package com.Lucca.Projeto1.mapper;

import com.Lucca.Projeto1.dto.movimentacao.MovimentacaoResponse;
import com.Lucca.Projeto1.dto.notafiscal.ItemNotaFiscalResponse;
import com.Lucca.Projeto1.dto.notafiscal.NotaFiscalResponse;
import com.Lucca.Projeto1.model.ItemNotaFiscal;
import com.Lucca.Projeto1.model.NotaFiscalEntrada;

import java.math.BigDecimal;
import java.util.List;

public final class NotaFiscalEntradaMapper {

    private NotaFiscalEntradaMapper() {
    }

    public static NotaFiscalResponse paraResponse(
            NotaFiscalEntrada notaFiscal,
            List<MovimentacaoResponse> movimentacoes
    ) {
        List<ItemNotaFiscalResponse> itens = notaFiscal.getItens()
                .stream()
                .map(NotaFiscalEntradaMapper::paraItemResponse)
                .toList();

        BigDecimal valorTotal = itens.stream()
                .map(ItemNotaFiscalResponse::getValorTotal)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);

        return new NotaFiscalResponse(
                notaFiscal.getId(),
                notaFiscal.getNumero(),
                notaFiscal.getSerie(),
                notaFiscal.getChaveAcesso(),
                notaFiscal.getFornecedor(),
                notaFiscal.getCnpjFornecedor(),
                notaFiscal.getDataEmissao(),
                notaFiscal.getDataEntrada(),
                notaFiscal.getStatus(),
                notaFiscal.getCadastradaPor().getId(),
                notaFiscal.getCadastradaPor().getUsername(),
                notaFiscal.getDataCadastro(),
                notaFiscal.getCaminhoArquivo(),
                itens,
                valorTotal,
                movimentacoes
        );
    }

    private static ItemNotaFiscalResponse paraItemResponse(
            ItemNotaFiscal item
    ) {
        BigDecimal valorTotal = item.getValorUnitario()
                .multiply(BigDecimal.valueOf(item.getQuantidade()));

        return new ItemNotaFiscalResponse(
                item.getId(),
                item.getMaterial().getId(),
                item.getMaterial().getNome(),
                item.getQuantidade(),
                item.getValorUnitario(),
                valorTotal
        );
    }
}
