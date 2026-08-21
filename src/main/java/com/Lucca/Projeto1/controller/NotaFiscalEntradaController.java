package com.Lucca.Projeto1.controller;

import com.Lucca.Projeto1.dto.notafiscal.NotaFiscalRequest;
import com.Lucca.Projeto1.dto.notafiscal.NotaFiscalResponse;
import com.Lucca.Projeto1.service.NotaFiscalEntradaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notas-fiscais")
public class NotaFiscalEntradaController {

    private final NotaFiscalEntradaService notaFiscalService;

    public NotaFiscalEntradaController(
            NotaFiscalEntradaService notaFiscalService
    ) {
        this.notaFiscalService = notaFiscalService;
    }

    @PostMapping
    public ResponseEntity<NotaFiscalResponse> criar(
            @Valid @RequestBody NotaFiscalRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(notaFiscalService.criar(request));
    }

    @GetMapping
    public ResponseEntity<List<NotaFiscalResponse>> listarTodas() {
        return ResponseEntity.ok(notaFiscalService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotaFiscalResponse> buscarPorId(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(notaFiscalService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NotaFiscalResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody NotaFiscalRequest request
    ) {
        return ResponseEntity.ok(notaFiscalService.atualizar(id, request));
    }

    @PostMapping("/{id}/confirmar")
    public ResponseEntity<NotaFiscalResponse> confirmarEntrada(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(notaFiscalService.confirmarEntrada(id));
    }
}
