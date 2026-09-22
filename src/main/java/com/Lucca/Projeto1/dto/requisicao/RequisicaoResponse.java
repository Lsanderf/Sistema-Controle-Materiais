package com.Lucca.Projeto1.dto.requisicao;

import com.Lucca.Projeto1.dto.movimentacao.MovimentacaoResponse;
import com.Lucca.Projeto1.model.StatusRequisicao;

import java.time.LocalDateTime;
import java.util.List;

public record RequisicaoResponse(
        Long id,
        String descricao,
        UsuarioResumo gerente,
        UsuarioResumo encarregado,
        ContratoResumo contrato,
        StatusRequisicao status,
        LocalDateTime criadoEm,
        LocalDateTime confirmadoEm,
        UsuarioResumo confirmadoPor,
        List<MovimentacaoResponse> movimentacoes
) {
    public record UsuarioResumo(Long id, String nome, String username) {}
    public record ContratoResumo(Long id, String nome, String descricao) {}
}
