package com.Lucca.Projeto1.dto.requisicao;

import com.Lucca.Projeto1.model.StatusRequisicao;
import com.Lucca.Projeto1.model.TipoMovimentacao;

import java.time.LocalDateTime;
import java.util.List;

public record RequisicaoResponse(
        Long id,
        UsuarioResumo gerenteSolicitante,
        UsuarioResumo encarregadoDestinatario,
        ContratoResumo contrato,
        TipoMovimentacao tipo,
        String observacao,
        StatusRequisicao status,
        LocalDateTime criadaEm,
        LocalDateTime visualizadaEm,
        LocalDateTime concluidaEm,
        List<Item> itens
) {
    public record UsuarioResumo(Long id, String nome) { }
    public record ContratoResumo(Long id, String nome) { }
    public record Item(Long id, String descricao, Integer quantidade) { }
}
