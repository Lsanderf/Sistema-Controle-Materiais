package com.Lucca.Projeto1.mapper;

import com.Lucca.Projeto1.dto.movimentacao.MovimentacaoResponse;
import com.Lucca.Projeto1.model.Movimentacao;

public class MovimentacaoMapper {

    private MovimentacaoMapper() {
    }

    public static MovimentacaoResponse paraResponse(
            Movimentacao movimentacao
    ) {
        return paraResponse(movimentacao, null);
    }

    public static MovimentacaoResponse paraResponse(
            Movimentacao movimentacao,
            Long estornoId
    ) {
        Long encarregadoId = movimentacao.getEncarregado() != null
                ? movimentacao.getEncarregado().getId()
                : null;

        String nomeEncarregado =
                movimentacao.getEncarregado() != null
                        ? movimentacao.getEncarregado().getNome()
                        : null;

        String nomeContrato =
                movimentacao.getContrato() != null
                        ? movimentacao.getContrato().getNome()
                        : null;

        String nomeMaterial =
                movimentacao.getMaterial() != null
                        ? movimentacao.getMaterial().getNome()
                        : null;

        Long usuarioId =
                movimentacao.getRegistradoPor() != null
                        ? movimentacao.getRegistradoPor().getId()
                        : null;

        String usuarioUsername =
                movimentacao.getRegistradoPor() != null
                        ? movimentacao.getRegistradoPor().getUsername()
                        : null;

        Long notaFiscalId =
                movimentacao.getNotaFiscal() != null
                        ? movimentacao.getNotaFiscal().getId()
                        : null;

        Long movimentacaoOrigemId =
                movimentacao.getMovimentacaoOrigem() != null
                        ? movimentacao.getMovimentacaoOrigem().getId()
                        : null;

        Long requisicaoId = movimentacao.getRequisicao() != null
                ? movimentacao.getRequisicao().getId()
                : null;

        return new MovimentacaoResponse(
                movimentacao.getId(),
                encarregadoId,
                nomeEncarregado,
                nomeContrato,
                nomeMaterial,
                movimentacao.getQuantidade(),
                movimentacao.getTipo(),
                movimentacao.getDataMovimentacao(),
                movimentacao.getDataFinalizacao(),
                usuarioId,
                usuarioUsername,
                notaFiscalId,
                movimentacao.getObservacao(),
                movimentacaoOrigemId,
                estornoId,
                requisicaoId
        );
    }
}
