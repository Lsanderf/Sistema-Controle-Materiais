package com.Lucca.Projeto1.service;

import com.Lucca.Projeto1.model.Contrato;
import com.Lucca.Projeto1.repository.ContratoRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ContratoService {

    private final ContratoRepository contratoRepository;

    ContratoService(ContratoRepository contratoRepository) {
        this.contratoRepository = contratoRepository;
    }

    public List<Contrato> buscarTodos(){
        return contratoRepository.findAll();
    }


    //Busca pelo ID
    public Contrato buscarPorId(Long id){
        return contratoRepository.
                findById(id).orElseThrow(() ->
                        new RuntimeException("Contrato não encontrado!"));
    }

    //Busca pelo Nome
    public Contrato buscaPeloNome(String nome){
        return contratoRepository.
                findByNomeIgnoreCase(nome).orElseThrow(() ->
                        new RuntimeException("Contrato não encontrado!"));
    }

    //Adicionar Contrato
    public Contrato adicionaContrato(Contrato contrato){
        Optional<Contrato> novoContrato = contratoRepository.findByNomeIgnoreCase(contrato.getNome());
        if(novoContrato.isPresent()) throw new RuntimeException("Contrato já existe no sistema!");
        return contratoRepository.save(contrato);
    }

    //Atualizar contrato

    public Contrato atualizarContrato(Long id, Contrato contrato){
        Contrato novoContrato = contratoRepository.
                findById(id).orElseThrow(() ->
                        new RuntimeException("Contrato não encontrado!"));
        novoContrato.setNome(contrato.getNome());
        novoContrato.setDescricao(contrato.getDescricao());
        novoContrato.setAtivo(contrato.getAtivo());

        return contratoRepository.save(novoContrato);
    }

    //Deletar
    public void deletarContrato(Long id){
        Contrato contrato = contratoRepository.
                findById(id).orElseThrow(() ->
                        new RuntimeException("Contrato não encontrado!"));
        contratoRepository.delete(contrato);
    }



}
