package com.Lucca.Projeto1.service;

import java.util.List;

import com.Lucca.Projeto1.dto.atualizacaoMaterial.MaterialAtualizacaoRequest;
import org.springframework.stereotype.Service;

import com.Lucca.Projeto1.dto.material.MaterialRequest;
import com.Lucca.Projeto1.dto.material.MaterialResponse;
import com.Lucca.Projeto1.exception.RecursoNaoEncontradoException;
import com.Lucca.Projeto1.exception.RegraNegocioException;
import com.Lucca.Projeto1.mapper.MaterialMapper;
import com.Lucca.Projeto1.model.Material;
import com.Lucca.Projeto1.repository.MaterialRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaterialService {
    private final MaterialRepository materialRepository;

    public MaterialService(MaterialRepository materialRepository){
        this.materialRepository = materialRepository;
    }


    @Transactional
    public MaterialResponse adicionarMaterial(MaterialRequest request){
        materialRepository.findByNomeIgnoreCase(request.getNome())
                .ifPresent(material -> {
                    throw new RegraNegocioException(
                            "Já existe um material com esse nome"
                    );
                });

        Material material = MaterialMapper.paraEntidade(request);
        Material materialSalvo = materialRepository.save(material);

        return MaterialMapper.paraResponse(materialSalvo);
    }

    public List<MaterialResponse> listarTodos() {
        return materialRepository.findAll()
                .stream()
                .map(MaterialMapper::paraResponse)
                .toList();
    }

    public MaterialResponse buscarPorId(Long id) {
        return materialRepository.findById(id)
                .map(MaterialMapper::paraResponse)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException("Material nao encontrado")
                );
    }

    public MaterialResponse atualizarMaterial(
            Long id,
            MaterialAtualizacaoRequest request
    ) {
        Material materialExistente = materialRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Material com ID " + id + " não encontrado"
                        )
                );

        materialRepository.findByNomeIgnoreCase(request.getNome())
                .filter(outroMaterial ->
                        !outroMaterial.getId().equals(materialExistente.getId())
                )
                .ifPresent(outroMaterial -> {
                    throw new RegraNegocioException(
                            "Já existe outro material com esse nome"
                    );
                });

        MaterialMapper.atualizarEntidade(
                request,
                materialExistente
        );


        Material materialAtualizado =
                materialRepository.save(materialExistente);

        return MaterialMapper.paraResponse(materialAtualizado);
    }


}
