package com.Lucca.Projeto1.dto.solicitacaoretirada;

import com.Lucca.Projeto1.model.StatusSolicitacaoRetirada;

import java.time.LocalDateTime;
import java.util.List;

public record SolicitacaoRetiradaResponse(
        Long id,
        UsuarioResumo operadorResponsavel,
        UsuarioResumo encarregadoAssinante,
        ContratoResumo contrato,
        StatusSolicitacaoRetirada status,
        String observacao,
        LocalDateTime criadaEm,
        LocalDateTime confirmadaEm,
        List<Item> itens
) {
    public record UsuarioResumo(Long id, String nome) { }
    public record ContratoResumo(Long id, String nome) { }
    public record Item(Long id, Long materialId, String material, Integer quantidade) { }
}
