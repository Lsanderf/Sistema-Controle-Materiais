package com.Lucca.Projeto1.service;

import com.Lucca.Projeto1.exception.RecursoNaoEncontradoException;
import com.Lucca.Projeto1.exception.RegraNegocioException;
import com.Lucca.Projeto1.model.Funcionario;
import com.Lucca.Projeto1.repository.FuncionarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;

    FuncionarioService(FuncionarioRepository funcionarioRepository){
        this.funcionarioRepository = funcionarioRepository;
    }

    public Funcionario adicionarFuncionario(Funcionario novofuncionario){
        Optional<Funcionario> funcionarioExistente= funcionarioRepository.findByCpf(novofuncionario.getCpf());

        if(funcionarioExistente.isPresent()) {
            throw new RegraNegocioException("Ja existe um funcionario com esse CPF!");
        }

        return funcionarioRepository.save(novofuncionario);
    }

    public void deletarFuncionario(Long id){
        Funcionario funcionario = funcionarioRepository.
                findById(id).orElseThrow(() ->
                        new RecursoNaoEncontradoException("Funcionario nao encontrado"));
        funcionarioRepository.delete(funcionario);
    }

    public Funcionario alterarFuncionario(Long id, Funcionario novofuncionario){
        Funcionario funcionario = funcionarioRepository.
                findById(id).orElseThrow(() ->
                        new RecursoNaoEncontradoException("Funcionario nao encontrado"));
        funcionario.setNome(novofuncionario.getNome());
        funcionario.setCargo(novofuncionario.getCargo());
        funcionario.setCpf(novofuncionario.getCpf());
        return funcionarioRepository.save(funcionario);
    }

    public Funcionario buscarPorId(Long id){
        return funcionarioRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Funcionario com ID " + id + " nao encontrado"
                        )
                );
    }

    public List<Funcionario> buscarTodos(){
        return funcionarioRepository.findAll();
    }





}
