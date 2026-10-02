package com.Lucca.Projeto1.dto.solicitacaoretirada;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SolicitacaoRetiradaRequest(
        @NotNull(message = "O encarregado assinante Ã© obrigatÃ³rio") @Positive Long encarregadoAssinanteId,
        @Positive(message = "O contrato deve ser maior que zero") Long contratoId,
        @Size(max = 1000, message = "A observaÃ§Ã£o deve possuir no mÃ¡ximo 1.000 caracteres") String observacao,
        @NotNull(message = "Informe ao menos um material")
        @Size(min = 1, max = 100, message = "Informe entre 1 e 100 materiais")
        List<@Valid Item> itens
) {
    public record Item(
            @NotNull(message = "O material Ã© obrigatÃ³rio") @Positive Long materialId,
            @NotNull(message = "A quantidade Ã© obrigatÃ³ria") @Positive @Max(value = 10000, message = "A quantidade mÃ¡xima por item Ã© 10.000") Integer quantidade
    ) { }
}
