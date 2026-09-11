package com.Lucca.Projeto1.dto.movimentacao;

import com.Lucca.Projeto1.model.TipoEvidenciaMovimentacao;

import java.time.LocalDateTime;

public record EvidenciaMovimentacaoResponse(
        Long id,
        TipoEvidenciaMovimentacao tipo,
        LocalDateTime dataEvidencia,
        FuncionarioResumoResponse funcionario,
        UsuarioResumoResponse registradaPor,
        String nomeArquivo,
        String contentType,
        Long tamanhoBytes,
        String sha256,
        String urlArquivo
) {
    public record FuncionarioResumoResponse(Long id, String nome) {
    }

    public record UsuarioResumoResponse(Long id, String username) {
    }
}
