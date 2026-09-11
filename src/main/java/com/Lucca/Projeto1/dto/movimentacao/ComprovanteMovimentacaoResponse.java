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
