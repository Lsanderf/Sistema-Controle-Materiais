package com.Lucca.Projeto1.service;

import java.util.List;

import com.Lucca.Projeto1.dto.AtualizacaoMaterial.MaterialAtualizacaoRequest;
import com.Lucca.Projeto1.exception.RegraNegocioException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import com.Lucca.Projeto1.dto.Material.MaterialRequest;
import com.Lucca.Projeto1.dto.Material.MaterialResponse;
import com.Lucca.Projeto1.exception.RecursoNaoEncontradoException;
import com.Lucca.Projeto1.mapper.MaterialMapper;
import com.Lucca.Projeto1.model.Material;
import com.Lucca.Projeto1.repository.MaterialRepository;

@Service
public class MaterialService {
    private final MaterialRepository materialRepository;

    public MaterialService(MaterialRepository materialRepository){
        this.materialRepository = materialRepository;
    }


    public MaterialResponse adicionarMaterial(MaterialRequest request){
        Material materialRecebido = MaterialMapper.paraEntidade(request);

        Material materialExistente = materialRepository.
                                    findByNomeIgnoreCase(materialRecebido.getNome()).
                                    orElse(null);

        if(materialExistente != null){
            int novaQuantidade = materialExistente.getQuantidadeEstoque() + materialRecebido.getQuantidadeEstoque();

            materialExistente.setQuantidadeEstoque(novaQuantidade);

            Material materialSalvo = materialRepository.save(materialExistente);

            return MaterialMapper.paraResponse(materialSalvo);
        }
        Material materialSalvo = materialRepository.save(materialRecebido);

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


        MaterialMapper.atualizarEntidade(
                request,
                materialExistente
        );


        Material materialAtualizado =
                materialRepository.save(materialExistente);

        return MaterialMapper.paraResponse(materialAtualizado);
    }


}
