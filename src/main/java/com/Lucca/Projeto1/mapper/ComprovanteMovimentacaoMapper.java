package com.Lucca.Projeto1.mapper;

import com.Lucca.Projeto1.dto.movimentacao.ComprovanteMovimentacaoResponse;
import com.Lucca.Projeto1.dto.movimentacao.EvidenciaMovimentacaoResponse;
import com.Lucca.Projeto1.model.AssinaturaRetirada;
import com.Lucca.Projeto1.model.ComprovanteMovimentacao;

import java.util.List;

public final class ComprovanteMovimentacaoMapper {

    private ComprovanteMovimentacaoMapper() {
    }

    public static ComprovanteMovimentacaoResponse paraResponse(
            ComprovanteMovimentacao comprovante,
            List<EvidenciaMovimentacaoResponse> evidencias,
            AssinaturaRetirada assinaturaRetirada
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

        ComprovanteMovimentacaoResponse.UsuarioResponsavelResumoResponse operadorResponsavel =
                comprovante.getOperadorResponsavelId() == null ? null
                        : new ComprovanteMovimentacaoResponse.UsuarioResponsavelResumoResponse(
                                comprovante.getOperadorResponsavelId(), comprovante.getOperadorResponsavelNome());
        ComprovanteMovimentacaoResponse.UsuarioResponsavelResumoResponse encarregadoAssinante =
                comprovante.getEncarregadoAssinanteId() == null ? null
                        : new ComprovanteMovimentacaoResponse.UsuarioResponsavelResumoResponse(
                                comprovante.getEncarregadoAssinanteId(), comprovante.getEncarregadoAssinanteNome());
        ComprovanteMovimentacaoResponse.AssinaturaRetiradaResponse assinatura =
                assinaturaRetirada == null ? null
                        : new ComprovanteMovimentacaoResponse.AssinaturaRetiradaResponse(
                                new ComprovanteMovimentacaoResponse.UsuarioResponsavelResumoResponse(
                                        assinaturaRetirada.getEncarregadoAssinante().getId(),
                                        assinaturaRetirada.getEncarregadoAssinante().getNome()
                                ),
                                assinaturaRetirada.getDataAssinatura(),
                                assinaturaRetirada.getContentType(),
                                assinaturaRetirada.getTamanhoBytes(),
                                assinaturaRetirada.getSha256(),
                                "/movimentacoes/" + comprovante.getMovimentacaoId()
                                        + "/assinatura-retirada/arquivo"
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
                comprovante.getSolicitacaoRetiradaId(),
                operadorResponsavel,
                encarregadoAssinante,
                assinatura,
                evidencias,
                comprovante.getGeradoEm(),
                comprovante.getVersao()
        );
    }
}
