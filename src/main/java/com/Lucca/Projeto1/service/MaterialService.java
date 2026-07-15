package com.Lucca.Projeto1.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.Lucca.Projeto1.model.Material;
import com.Lucca.Projeto1.repository.MaterialRepository;

@Service
public class MaterialService {
    private final MaterialRepository materialRepository;

    public MaterialService(MaterialRepository materialRepository){
        this.materialRepository = materialRepository;
    }


    public Material adicionarMaterial(Material materialRecebido){
        Material materialExistente = materialRepository.
                                    findByNomeIgnoreCase(materialRecebido.getNome()).
                                    orElse(null);

        if(materialExistente != null){
            int novaQuantidade = materialExistente.getQuantidadeEstoque() + materialRecebido.getQuantidadeEstoque();

            materialExistente.setQuantidadeEstoque(novaQuantidade);

            return materialRepository.save(materialExistente);
        }
        return materialRepository.save(materialRecebido);
    }

     public List<Material> listarTodos() {
        return materialRepository.findAll();
    }

    public Material buscarPorId(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Material não encontrado")
                );
    }

    public Material atualizarMaterial(Long id, Material novosDados) {

    Material materialExistente = materialRepository.findById(id)
            .orElseThrow(() ->
                    new RuntimeException("Material não encontrado")
            );

    materialExistente.setNome(novosDados.getNome());
    materialExistente.setDescricao(novosDados.getDescricao());
    materialExistente.setQuantidadeEstoque(
            novosDados.getQuantidadeEstoque()
    );

    return materialRepository.save(materialExistente);
}

    public void excluir(Long id) {
        Material material = buscarPorId(id);
        materialRepository.delete(material);
    }

}
