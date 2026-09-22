package com.Lucca.Projeto1.dto.movimentacao;

import com.Lucca.Projeto1.model.TipoEvidenciaMovimentacao;

import java.time.LocalDateTime;

public record EvidenciaMovimentacaoResponse(
        Long id,
        TipoEvidenciaMovimentacao tipo,
        LocalDateTime dataEvidencia,
        UsuarioResumoResponse encarregado,
        UsuarioResumoResponse assinante,
        UsuarioResumoResponse registradaPor,
        String nomeArquivo,
        String contentType,
        Long tamanhoBytes,
        String sha256,
        String urlArquivo
) {
    public record UsuarioResumoResponse(Long id, String nome, String username) {
    }
}
