package com.Lucca.Projeto1.mapper;

import com.Lucca.Projeto1.dto.ContratoRequest;
import com.Lucca.Projeto1.dto.ContratoResponse;
import com.Lucca.Projeto1.model.Contrato;

public class ContratoMapper {

    private ContratoMapper() {
    }

    public static Contrato paraEntidade(
            ContratoRequest request
    ) {
        Contrato contrato = new Contrato();

        contrato.setNome(request.getNome());
        contrato.setDescricao(request.getDescricao());
        contrato.setAtivo(request.getAtivo());

        return contrato;
    }

    public static ContratoResponse paraResponse(
            Contrato contrato
    ) {
        return new ContratoResponse(
                contrato.getId(),
                contrato.getNome(),
                contrato.getDescricao(),
                contrato.getAtivo()
        );
    }

    public static void atualizarEntidade(
            ContratoRequest request,
            Contrato contrato
    ) {
        contrato.setNome(request.getNome());
        contrato.setDescricao(request.getDescricao());
        contrato.setAtivo(request.getAtivo());
    }
}
