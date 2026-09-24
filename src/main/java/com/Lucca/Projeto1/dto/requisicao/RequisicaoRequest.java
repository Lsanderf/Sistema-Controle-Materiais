package com.Lucca.Projeto1.dto.requisicao;

import com.Lucca.Projeto1.model.TipoMovimentacao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RequisicaoRequest(
        @NotNull(message = "O encarregado destinatário é obrigatório") Long encarregadoDestinatarioId,
        @NotNull(message = "O contrato é obrigatório") Long contratoId,
        @NotNull(message = "O tipo é obrigatório") TipoMovimentacao tipo,
        @Size(max = 1000, message = "A observação deve ter no máximo 1.000 caracteres") String observacao,
        @NotNull(message = "Informe ao menos um material")
        @Size(min = 1, max = 100, message = "Informe entre 1 e 100 materiais")
        List<@Valid RequisicaoItemRequest> itens
) { }
