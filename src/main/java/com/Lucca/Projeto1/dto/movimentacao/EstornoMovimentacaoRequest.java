package com.Lucca.Projeto1.dto.movimentacao;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EstornoMovimentacaoRequest {

    @NotBlank(message = "A justificativa do estorno é obrigatória")
    @Size(max = 1000, message = "A justificativa deve possuir no máximo 1.000 caracteres")
    private String justificativa;

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }

    @JsonAnySetter
    public void rejeitarCampoDesconhecido(String campo, Object valor) {
        throw new IllegalArgumentException("Campo não permitido: " + campo);
    }
}
