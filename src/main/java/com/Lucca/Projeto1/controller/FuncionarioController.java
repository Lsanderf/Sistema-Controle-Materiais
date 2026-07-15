package com.Lucca.Projeto1.controller;

import com.Lucca.Projeto1.model.Funcionario;
import com.Lucca.Projeto1.service.FuncionarioService;
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
    public List<Funcionario> buscarTodos(){
        return funcionarioService.buscarTodos();
    }

    @GetMapping("/{id}")
    public Funcionario buscarPeloId(@PathVariable long id){
        return funcionarioService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<Funcionario> adicionarFuncionario(@RequestBody Funcionario funcionario){
        Funcionario novoFuncionario = funcionarioService.adicionarFuncionario(funcionario);
        return ResponseEntity.ok(novoFuncionario);

    }

    @PutMapping("/{id}")
    public ResponseEntity<Funcionario> atualizarFuncionario(@PathVariable Long id, @RequestBody Funcionario novosDados){
        Funcionario funcionarioAtualizado = funcionarioService.alterarFuncionario(id, novosDados);
        return ResponseEntity.ok(funcionarioAtualizado);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id){
        funcionarioService.deletarFuncionario(id);
    }

}
