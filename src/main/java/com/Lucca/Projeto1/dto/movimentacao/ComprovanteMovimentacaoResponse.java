package com.Lucca.Projeto1.dto.movimentacao;

import com.Lucca.Projeto1.model.TipoMovimentacao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ComprovanteMovimentacaoResponse(
        Long id,
        TipoMovimentacao tipo,
        Integer quantidade,
        LocalDateTime dataMovimentacao,
        LocalDateTime dataFinalizacao,
        String observacao,
        MaterialResumoResponse material,
        FuncionarioResumoResponse funcionario,
        ContratoResumoResponse contrato,
        UsuarioResumoResponse registradoPor,
        NotaFiscalResumoResponse notaFiscal,
        Long movimentacaoOrigemId,
        Long solicitacaoRetiradaId,
        UsuarioResponsavelResumoResponse operadorResponsavel,
        UsuarioResponsavelResumoResponse encarregadoAssinante,
        AssinaturaRetiradaResponse assinaturaRetirada,
        List<EvidenciaMovimentacaoResponse> evidencias,
        LocalDateTime geradoEm,
        Integer versao
) {
    public record MaterialResumoResponse(
            Long id,
            String nome,
            String descricao
    ) {
    }

    public record FuncionarioResumoResponse(
            Long id,
            String nome,
            String cargo
    ) {
    }

    public record ContratoResumoResponse(
            Long id,
            String nome,
            String descricao
    ) {
    }

    public record UsuarioResumoResponse(Long id, String username) {
    }

    public record UsuarioResponsavelResumoResponse(Long id, String nome) {
    }

    public record AssinaturaRetiradaResponse(
            UsuarioResponsavelResumoResponse encarregadoAssinante,
            LocalDateTime dataAssinatura,
            String contentType,
            Long tamanhoBytes,
            String sha256,
            String urlArquivo
    ) {
    }

    public record NotaFiscalResumoResponse(
            Long id,
            String numero,
            String serie,
            String chaveAcesso,
            String fornecedor,
            String cnpjFornecedor,
            LocalDate dataEmissao,
            LocalDateTime dataEntrada
    ) {
    }
}
