package com.Lucca.Projeto1.controller;

import com.Lucca.Projeto1.model.Contrato;
import com.Lucca.Projeto1.service.ContratoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contratos")
public class ContratoController {
    private final ContratoService contratoService;

    ContratoController(ContratoService contratoService){
        this.contratoService = contratoService;
    }

    @GetMapping
    public List<Contrato> buscarTodos(){
        return contratoService.buscarTodos();
    }

    @GetMapping("/{id}")
    public Contrato buscarPorId(@PathVariable Long id){
        return contratoService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<Contrato> adicionar(@RequestBody Contrato contrato){
       Contrato novoContrato = contratoService.adicionaContrato(contrato);
       return ResponseEntity.ok(novoContrato);
    }

    @PutMapping ("/{id}")
    public ResponseEntity<Contrato> atualizar(@PathVariable Long id, @RequestBody Contrato novosDados){
        Contrato novoContrato = contratoService.atualizarContrato(id, novosDados);
        return ResponseEntity.ok(novoContrato);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id){
        contratoService.deletarContrato(id);
    }
}
