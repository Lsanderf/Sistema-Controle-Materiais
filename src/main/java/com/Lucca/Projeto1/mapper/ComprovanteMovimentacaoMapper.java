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
        ComprovanteMovimentacaoResponse.EncarregadoResumoResponse encarregado =
                comprovante.getEncarregadoId() == null
                        ? null
                        : new ComprovanteMovimentacaoResponse.EncarregadoResumoResponse(
                                comprovante.getEncarregadoId(),
                                comprovante.getEncarregadoNome()
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
                encarregado,
                contrato,
                registradoPor,
                notaFiscal,
                comprovante.getMovimentacaoOrigemId(),
                comprovante.getRequisicaoId() == null ? null
                        : new ComprovanteMovimentacaoResponse.RequisicaoResumoResponse(
                                comprovante.getRequisicaoId(),
                                comprovante.getRequisicaoDescricao()),
                evidencias,
                comprovante.getGeradoEm(),
                comprovante.getVersao()
        );
    }
}
