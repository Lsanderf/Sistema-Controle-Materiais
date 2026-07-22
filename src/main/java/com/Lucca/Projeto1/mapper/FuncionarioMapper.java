package com.Lucca.Projeto1.mapper;

import com.Lucca.Projeto1.dto.Funcionario.FuncionarioRequest;
import com.Lucca.Projeto1.dto.Funcionario.FuncionarioResponse;
import com.Lucca.Projeto1.model.Funcionario;

public class FuncionarioMapper {

    private FuncionarioMapper() {
    }

    public static Funcionario paraEntidade(
            FuncionarioRequest request
    ) {
        Funcionario funcionario = new Funcionario();

        funcionario.setNome(request.getNome());
        funcionario.setCpf(request.getCpf());
        funcionario.setCargo(request.getCargo());

        return funcionario;
    }

    public static FuncionarioResponse paraResponse(
            Funcionario funcionario
    ) {
        return new FuncionarioResponse(
                funcionario.getId(),
                funcionario.getNome(),
                funcionario.getCargo(),
                funcionario.isAtivo()
        );
    }

    public static void atualizarEntidade(
            FuncionarioRequest request,
            Funcionario funcionario
    ) {
        funcionario.setNome(request.getNome());
        funcionario.setCpf(request.getCpf());
        funcionario.setCargo(request.getCargo());
    }
}