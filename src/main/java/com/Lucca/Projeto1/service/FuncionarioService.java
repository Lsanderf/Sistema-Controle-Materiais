package com.Lucca.Projeto1.service;

import com.Lucca.Projeto1.dto.FuncionarioRequest;
import com.Lucca.Projeto1.dto.FuncionarioResponse;
import com.Lucca.Projeto1.exception.RecursoNaoEncontradoException;
import com.Lucca.Projeto1.exception.RegraNegocioException;
import com.Lucca.Projeto1.mapper.FuncionarioMapper;
import com.Lucca.Projeto1.model.Funcionario;
import com.Lucca.Projeto1.repository.FuncionarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;

    FuncionarioService(FuncionarioRepository funcionarioRepository){
        this.funcionarioRepository = funcionarioRepository;
    }

    public FuncionarioResponse adicionarFuncionario(
            FuncionarioRequest request
    ) {
        if (funcionarioRepository
                .findByCpf(request.getCpf())
                .isPresent()) {

            throw new RegraNegocioException(
                    "Já existe um funcionário com esse CPF"
            );
        }

        Funcionario funcionario = FuncionarioMapper.paraEntidade(request);

        Funcionario funcionarioSalvo =
                funcionarioRepository.save(funcionario);

        return converterParaResponse(funcionarioSalvo);
    }

    public void deletarFuncionario(Long id){
        Funcionario funcionario = funcionarioRepository.
                findById(id).orElseThrow(() ->
                        new RecursoNaoEncontradoException("Funcionario nao encontrado"));
        funcionarioRepository.delete(funcionario);
    }

    private FuncionarioResponse converterParaResponse(
            Funcionario funcionario
    ) {
        return new FuncionarioResponse(
                funcionario.getId(),
                funcionario.getNome(),
                funcionario.getCargo()
        );
    }

    public FuncionarioResponse atualizarFuncionario(
            Long id,
            FuncionarioRequest request
    ) {
        Funcionario funcionario = funcionarioRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Funcionário com ID " + id
                                        + " não encontrado"
                        )
                );
        funcionarioRepository.findByCpf(request.getCpf())
                .filter(outroFuncionario ->
                        !Objects.equals(outroFuncionario.getId(), id)
                )
                .ifPresent(outroFuncionario -> {
                    throw new RegraNegocioException(
                            "Já existe outro funcionário com esse CPF"
                    );
                });

        FuncionarioMapper.atualizarEntidade(
                request,
                funcionario
        );


        Funcionario funcionarioAtualizado =
                funcionarioRepository.save(funcionario);

        return FuncionarioMapper.paraResponse(
                funcionarioAtualizado
        );
    }

    public FuncionarioResponse buscarPorId(Long id) {
        return funcionarioRepository.findById(id)
                .map(FuncionarioMapper::paraResponse)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Funcionário com ID " + id
                                        + " não encontrado"
                        )
                );
    }

    public List<FuncionarioResponse> listarTodos() {
        return funcionarioRepository.findAll()
                .stream()
                .map(FuncionarioMapper::paraResponse)
                .toList();
    }





}
