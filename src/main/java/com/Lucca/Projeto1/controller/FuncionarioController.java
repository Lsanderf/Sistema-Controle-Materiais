package com.Lucca.Projeto1.controller;

import com.Lucca.Projeto1.dto.Funcionario.FuncionarioRequest;
import com.Lucca.Projeto1.dto.Funcionario.FuncionarioResponse;
import com.Lucca.Projeto1.service.FuncionarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/funcionarios")
public class FuncionarioController {

    private final FuncionarioService funcionarioService;
    FuncionarioController(FuncionarioService funcionarioService){
        this.funcionarioService = funcionarioService;
    }

    @GetMapping
    public ResponseEntity<List<FuncionarioResponse>> listarTodos() {
        return ResponseEntity.ok(
                funcionarioService.listarTodos()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<FuncionarioResponse> buscarPorId(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                funcionarioService.buscarPorId(id)
        );
    }

    @PostMapping
    public ResponseEntity<FuncionarioResponse> cadastrar(
            @Valid @RequestBody FuncionarioRequest request
    ) {
        FuncionarioResponse funcionario =
                funcionarioService.adicionarFuncionario(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(funcionario);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FuncionarioResponse> atualizarFuncionario(
            @PathVariable Long id,
            @Valid @RequestBody FuncionarioRequest novosDados
    ) {
        FuncionarioResponse funcionarioAtualizado =
                funcionarioService.atualizarFuncionario(id, novosDados);

        return ResponseEntity.ok(funcionarioAtualizado);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id){
        funcionarioService.deletarFuncionario(id);
    }

}
