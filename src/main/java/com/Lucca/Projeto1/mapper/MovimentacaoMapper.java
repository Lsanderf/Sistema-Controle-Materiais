package com.Lucca.Projeto1.mapper;

import com.Lucca.Projeto1.dto.contrato.ContratoResponse;
import com.Lucca.Projeto1.dto.funcionario.FuncionarioResponse;
import com.Lucca.Projeto1.dto.material.MaterialResponse;
import com.Lucca.Projeto1.dto.movimentacao.MovimentacaoResponse;
import com.Lucca.Projeto1.model.Movimentacao;

public class MovimentacaoMapper {

    private MovimentacaoMapper() {
    }

    public static MovimentacaoResponse paraResponse(
            Movimentacao movimentacao
    ) {
        String nomeFuncionario =
                movimentacao.getFuncionario() != null
                        ? movimentacao.getFuncionario().getNome()
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

        return new MovimentacaoResponse(
                movimentacao.getId(),
                nomeFuncionario,
                nomeContrato,
                nomeMaterial,
                movimentacao.getQuantidade(),
                movimentacao.getTipo(),
                movimentacao.getDataMovimentacao(),
                movimentacao.getDataFinalizacao(),
                usuarioId,
                usuarioUsername,
                notaFiscalId,
                movimentacao.getObservacao()
        );
    }
}
