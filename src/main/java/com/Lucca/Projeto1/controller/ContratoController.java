package com.Lucca.Projeto1.controller;

import com.Lucca.Projeto1.dto.ContratoRequest;
import com.Lucca.Projeto1.dto.ContratoResponse;
import com.Lucca.Projeto1.service.ContratoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
    public List<ContratoResponse> buscarTodos(){
        return contratoService.buscarTodos();
    }

    @GetMapping("/{id}")
    public ContratoResponse buscarPorId(@PathVariable Long id){
        return contratoService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<ContratoResponse> adicionar(
            @Valid @RequestBody ContratoRequest request
    ){
       ContratoResponse novoContrato =
               contratoService.adicionaContrato(request);

       return ResponseEntity
               .status(HttpStatus.CREATED)
               .body(novoContrato);
    }

    @PutMapping ("/{id}")
    public ResponseEntity<ContratoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody ContratoRequest novosDados
    ){
        ContratoResponse novoContrato =
                contratoService.atualizarContrato(id, novosDados);

        return ResponseEntity.ok(novoContrato);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id){
        contratoService.deletarContrato(id);
    }
}
