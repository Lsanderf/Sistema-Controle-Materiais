package com.Lucca.Projeto1.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.Lucca.Projeto1.dto.MaterialRequest;
import com.Lucca.Projeto1.dto.MaterialResponse;
import com.Lucca.Projeto1.service.MaterialService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/materiais")
public class MaterialController {
    private final MaterialService materialService;
    
    MaterialController(MaterialService materialService){
        this.materialService = materialService;
    }

    @PostMapping
    public ResponseEntity<MaterialResponse> adicionar(
            @Valid @RequestBody MaterialRequest request
    ) {
        MaterialResponse materialSalvo =
                materialService.adicionarMaterial(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(materialSalvo);
    }

      // Listar todos os materiais
    @GetMapping
    public ResponseEntity<List<MaterialResponse>> listarTodos() {
        return ResponseEntity.ok(materialService.listarTodos());
    }

    // Buscar um material pelo ID
    @GetMapping("/{id}")
    public ResponseEntity<MaterialResponse> buscarPorId(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(materialService.buscarPorId(id));
    }

    // Excluir um material pelo ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        materialService.excluir(id);

        return ResponseEntity.noContent().build();
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<MaterialResponse> atualizarMaterial(
            @PathVariable Long id,
            @Valid @RequestBody MaterialRequest novosDados
    ) {

        MaterialResponse materialAtualizado =
                materialService.atualizarMaterial(id, novosDados);

        return ResponseEntity.ok(materialAtualizado);
    }



}
