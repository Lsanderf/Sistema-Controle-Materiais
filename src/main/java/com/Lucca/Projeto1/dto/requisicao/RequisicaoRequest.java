package com.Lucca.Projeto1.dto.requisicao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class RequisicaoRequest {
    @NotBlank(message = "A descrição é obrigatória")
    @Size(max = 4000, message = "A descrição deve possuir no máximo 4.000 caracteres")
    private String descricao;

    @NotNull(message = "O encarregado é obrigatório")
    @Positive(message = "O encarregado deve ser maior que zero")
    private Long encarregadoId;

    @NotNull(message = "O contrato é obrigatório")
    @Positive(message = "O contrato deve ser maior que zero")
    private Long contratoId;

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Long getEncarregadoId() { return encarregadoId; }
    public void setEncarregadoId(Long encarregadoId) { this.encarregadoId = encarregadoId; }
    public Long getContratoId() { return contratoId; }
    public void setContratoId(Long contratoId) { this.contratoId = contratoId; }
}
