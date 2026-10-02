package com.Lucca.Projeto1.dto.requisicao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RequisicaoFaltaEstoqueRequest(
        @NotNull @Positive Long gerenteDestinatarioId,
        @NotNull @Positive Long encarregadoNecessidadeId,
        @NotNull @Positive Long contratoId,
        @NotNull @Size(min = 1, max = 100) List<@Valid Item> itens
) {
    public record Item(
            @NotNull @Positive Long materialId,
            @NotNull @Positive @Max(10000) Integer quantidadeSolicitada
    ) { }
}
