package com.Lucca.Projeto1.mapper;

import com.Lucca.Projeto1.dto.AtualizacaoMaterial.MaterialAtualizacaoRequest;
import com.Lucca.Projeto1.dto.Material.MaterialRequest;
import com.Lucca.Projeto1.dto.Material.MaterialResponse;
import com.Lucca.Projeto1.model.Material;

public class MaterialMapper {

    private MaterialMapper() {
    }

    public static Material paraEntidade(
            MaterialRequest request
    ) {
        Material material = new Material();

        material.setNome(request.getNome());
        material.setDescricao(request.getDescricao());
        material.setQuantidadeEstoque(request.getQuantidadeEstoque());

        return material;
    }

    public static MaterialResponse paraResponse(
            Material material
    ) {
        return new MaterialResponse(
                material.getId(),
                material.getNome(),
                material.getDescricao(),
                material.getQuantidadeEstoque()
        );
    }



    public static void atualizarEntidade(
            MaterialAtualizacaoRequest request,
            Material material
    ) {
        material.setNome(request.getNome());
        material.setDescricao(request.getDescricao());
    }
}
