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
                new EvidenciaMovimentacaoResponse.FuncionarioResumoResponse(
                        evidencia.getFuncionarioId(),
                        evidencia.getFuncionarioNome()
                ),
                new EvidenciaMovimentacaoResponse.UsuarioResumoResponse(
                        evidencia.getRegistradaPorId(),
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
