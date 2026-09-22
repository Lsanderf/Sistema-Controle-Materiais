package com.Lucca.Projeto1.mapper;

import com.Lucca.Projeto1.dto.movimentacao.EvidenciaMovimentacaoResponse;
import com.Lucca.Projeto1.model.EvidenciaMovimentacao;

public final class EvidenciaMovimentacaoMapper {

    private EvidenciaMovimentacaoMapper() {
    }

    public static EvidenciaMovimentacaoResponse paraResponse(
            EvidenciaMovimentacao evidencia
    ) {
        return new EvidenciaMovimentacaoResponse(
                evidencia.getId(),
                evidencia.getTipo(),
                evidencia.getDataEvidencia(),
                new EvidenciaMovimentacaoResponse.UsuarioResumoResponse(
                        evidencia.getEncarregadoId(),
                        evidencia.getEncarregadoNome(),
                        null
                ),
                evidencia.getAssinanteId() == null ? null
                        : new EvidenciaMovimentacaoResponse.UsuarioResumoResponse(
                                evidencia.getAssinanteId(),
                                evidencia.getAssinanteNome(),
                                evidencia.getAssinanteUsername()),
                new EvidenciaMovimentacaoResponse.UsuarioResumoResponse(
                        evidencia.getRegistradaPorId(),
                        null,
                        evidencia.getRegistradaPorUsername()
                ),
                evidencia.getNomeArquivoOriginal(),
                evidencia.getContentType(),
                evidencia.getTamanhoBytes(),
                evidencia.getSha256(),
                "/movimentacoes/" + evidencia.getMovimentacaoId()
                        + "/evidencias/" + evidencia.getId() + "/arquivo"
        );
    }
}
