package com.Lucca.Projeto1.dto.requisicao;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RequisicaoItemRequest(
        @NotBlank(message = "A descrição do material é obrigatória")
        @Size(max = 255, message = "A descrição do material deve ter no máximo 255 caracteres")
        String descricao,
        @NotNull(message = "A quantidade é obrigatória")
        @Min(value = 1, message = "A quantidade deve ser maior que zero")
        @Max(value = 10000, message = "A quantidade máxima por item é 10.000") Integer quantidade
) { }
