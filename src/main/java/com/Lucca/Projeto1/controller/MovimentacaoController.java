package com.Lucca.Projeto1.controller;

import com.Lucca.Projeto1.dto.MovimentacaoRequest;
import com.Lucca.Projeto1.model.Movimentacao;
import com.Lucca.Projeto1.service.MovimentacaoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/movimentacoes")
public class MovimentacaoController {
    private final MovimentacaoService movimentacaoService;

    public MovimentacaoController(MovimentacaoService movimentacaoService){
        this.movimentacaoService = movimentacaoService;
    }

    @PostMapping
    public ResponseEntity<Movimentacao> registrar(
            @RequestBody MovimentacaoRequest request
    ) {
        Movimentacao movimentacao =
                movimentacaoService.registrarMovimentacao(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(movimentacao);
    }

    @GetMapping
    public ResponseEntity<List<Movimentacao>> listarTodas() {
        return ResponseEntity.ok(
                movimentacaoService.listarTodas()
        );
    }

    @GetMapping("/funcionario/{funcionarioId}")
    public ResponseEntity<List<Movimentacao>>
    listarPorFuncionario(
            @PathVariable Long funcionarioId
    ) {
        return ResponseEntity.ok(
                movimentacaoService
                        .listarPorFuncionario(funcionarioId)
        );
    }

    @GetMapping("/contrato/{contratoId}")
    public ResponseEntity<List<Movimentacao>>
    listarPorContrato(
            @PathVariable Long contratoId
    ) {
        return ResponseEntity.ok(
                movimentacaoService.listarPorContrato(contratoId)
        );
    }

    @GetMapping("/material/{materialId}")
    public ResponseEntity<List<Movimentacao>>
    listarPorMaterial(
            @PathVariable Long materialId
    ) {
        return ResponseEntity.ok(
                movimentacaoService.listarPorMaterial(materialId)
        );
    }

}
