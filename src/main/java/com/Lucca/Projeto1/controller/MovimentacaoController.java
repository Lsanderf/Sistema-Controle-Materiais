package com.Lucca.Projeto1.controller;

import com.Lucca.Projeto1.dto.MovimentacaoRequest;
import com.Lucca.Projeto1.service.MovimentacaoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.Lucca.Projeto1.dto.MovimentacaoResponse;
import java.util.List;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/movimentacoes")
public class MovimentacaoController {
    private final MovimentacaoService movimentacaoService;

    public MovimentacaoController(MovimentacaoService movimentacaoService){
        this.movimentacaoService = movimentacaoService;
    }

    @PostMapping
    public ResponseEntity<MovimentacaoResponse> registrar(
            @Valid @RequestBody MovimentacaoRequest request
    ) {
        MovimentacaoResponse response =
                movimentacaoService.registrarMovimentacao(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<MovimentacaoResponse>> listarTodas() {
        return ResponseEntity.ok(
                movimentacaoService.listarTodas()
        );
    }

    @GetMapping("/funcionario/{funcionarioId}")
    public ResponseEntity<List<MovimentacaoResponse>>
    listarPorFuncionario(
            @PathVariable Long funcionarioId
    ) {
        return ResponseEntity.ok(
                movimentacaoService
                        .listarPorFuncionario(funcionarioId)
        );
    }

    @GetMapping("/contrato/{contratoId}")
    public ResponseEntity<List<MovimentacaoResponse>>
    listarPorContrato(
            @PathVariable Long contratoId
    ) {
        return ResponseEntity.ok(
                movimentacaoService.listarPorContrato(contratoId)
        );
    }

    @GetMapping("/material/{materialId}")
    public ResponseEntity<List<MovimentacaoResponse>>
    listarPorMaterial(
            @PathVariable Long materialId
    ) {
        return ResponseEntity.ok(
                movimentacaoService.listarPorMaterial(materialId)
        );
    }

}
