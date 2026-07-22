package com.Lucca.Projeto1.service;

import com.Lucca.Projeto1.dto.ContratoRequest;
import com.Lucca.Projeto1.dto.ContratoResponse;
import com.Lucca.Projeto1.exception.RecursoNaoEncontradoException;
import com.Lucca.Projeto1.exception.RegraNegocioException;
import com.Lucca.Projeto1.mapper.ContratoMapper;
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

    public List<ContratoResponse> buscarTodos(){
        return contratoRepository.findAll()
                .stream()
                .map(ContratoMapper::paraResponse)
                .toList();
    }


    //Busca pelo ID
    public ContratoResponse buscarPorId(Long id){
        return contratoRepository.
                findById(id)
                .map(ContratoMapper::paraResponse)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException("Contrato nao encontrado!"));
    }

    //Busca pelo Nome
    public ContratoResponse buscaPeloNome(String nome){
        return contratoRepository.
                findByNomeIgnoreCase(nome)
                .map(ContratoMapper::paraResponse)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException("Contrato nao encontrado!"));
    }

    //Adicionar Contrato
    public ContratoResponse adicionaContrato(ContratoRequest request){
        Contrato contrato = ContratoMapper.paraEntidade(request);

        Optional<Contrato> novoContrato = contratoRepository.findByNomeIgnoreCase(contrato.getNome());
        if(novoContrato.isPresent()) throw new RegraNegocioException("Contrato ja existe no sistema!");
        Contrato contratoSalvo = contratoRepository.save(contrato);

        return ContratoMapper.paraResponse(contratoSalvo);
    }

    //Atualizar contrato

    public ContratoResponse atualizarContrato(Long id, ContratoRequest request){
        Contrato novoContrato = contratoRepository.
                findById(id).orElseThrow(() ->
                        new RecursoNaoEncontradoException("Contrato nao encontrado!"));
        ContratoMapper.atualizarEntidade(
                request,
                novoContrato
        );

        Contrato contratoAtualizado =
                contratoRepository.save(novoContrato);

        return ContratoMapper.paraResponse(contratoAtualizado);
    }

    //Deletar
    public void deletarContrato(Long id){
        Contrato contrato = contratoRepository.
                findById(id).orElseThrow(() ->
                        new RecursoNaoEncontradoException("Contrato nao encontrado!"));
        contratoRepository.delete(contrato);
    }



}
