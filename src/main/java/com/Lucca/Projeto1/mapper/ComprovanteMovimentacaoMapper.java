package com.Lucca.Projeto1.mapper;

import com.Lucca.Projeto1.dto.movimentacao.ComprovanteMovimentacaoResponse;
import com.Lucca.Projeto1.dto.movimentacao.EvidenciaMovimentacaoResponse;
import com.Lucca.Projeto1.model.ComprovanteMovimentacao;

import java.util.List;

public final class ComprovanteMovimentacaoMapper {

    private ComprovanteMovimentacaoMapper() {
    }

    public static ComprovanteMovimentacaoResponse paraResponse(
            ComprovanteMovimentacao comprovante,
            List<EvidenciaMovimentacaoResponse> evidencias
    ) {
        ComprovanteMovimentacaoResponse.FuncionarioResumoResponse funcionario =
                comprovante.getFuncionarioId() == null
                        ? null
                        : new ComprovanteMovimentacaoResponse.FuncionarioResumoResponse(
                                comprovante.getFuncionarioId(),
                                comprovante.getFuncionarioNome(),
                                comprovante.getFuncionarioCargo()
                        );

        ComprovanteMovimentacaoResponse.ContratoResumoResponse contrato =
                comprovante.getContratoId() == null
                        ? null
                        : new ComprovanteMovimentacaoResponse.ContratoResumoResponse(
                                comprovante.getContratoId(),
                                comprovante.getContratoNome(),
                                comprovante.getContratoDescricao()
                        );

        ComprovanteMovimentacaoResponse.UsuarioResumoResponse registradoPor =
                comprovante.getUsuarioId() == null
                        ? null
                        : new ComprovanteMovimentacaoResponse.UsuarioResumoResponse(
                                comprovante.getUsuarioId(),
                                comprovante.getUsuarioUsername()
                        );

        ComprovanteMovimentacaoResponse.NotaFiscalResumoResponse notaFiscal =
                comprovante.getNotaFiscalId() == null
                        ? null
                        : new ComprovanteMovimentacaoResponse.NotaFiscalResumoResponse(
                                comprovante.getNotaFiscalId(),
                                comprovante.getNotaFiscalNumero(),
                                comprovante.getNotaFiscalSerie(),
                                comprovante.getNotaFiscalChaveAcesso(),
                                comprovante.getNotaFiscalFornecedor(),
                                comprovante.getNotaFiscalCnpjFornecedor(),
                                comprovante.getNotaFiscalDataEmissao(),
                                comprovante.getNotaFiscalDataEntrada()
                        );

        return new ComprovanteMovimentacaoResponse(
                comprovante.getMovimentacaoId(),
                comprovante.getTipo(),
                comprovante.getQuantidade(),
                comprovante.getDataMovimentacao(),
                comprovante.getDataFinalizacao(),
                comprovante.getObservacao(),
                new ComprovanteMovimentacaoResponse.MaterialResumoResponse(
                        comprovante.getMaterialId(),
                        comprovante.getMaterialNome(),
                        comprovante.getMaterialDescricao()
                ),
                funcionario,
                contrato,
                registradoPor,
                notaFiscal,
                comprovante.getMovimentacaoOrigemId(),
                evidencias,
                comprovante.getGeradoEm(),
                comprovante.getVersao()
        );
    }
}
