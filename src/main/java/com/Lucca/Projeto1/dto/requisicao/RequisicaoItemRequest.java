package com.Lucca.Projeto1.dto.requisicao;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RequisicaoItemRequest(
        @NotNull(message = "O material é obrigatório") Long materialId,
        @NotNull(message = "A quantidade é obrigatória")
        @Min(value = 1, message = "A quantidade deve ser maior que zero")
        @Max(value = 10000, message = "A quantidade máxima por item é 10.000") Integer quantidade
) { }
