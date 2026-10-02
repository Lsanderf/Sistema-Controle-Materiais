package com.Lucca.Projeto1.controller;

import com.Lucca.Projeto1.dto.solicitacaoretirada.SolicitacaoRetiradaRequest;
import com.Lucca.Projeto1.dto.solicitacaoretirada.SolicitacaoRetiradaResponse;
import com.Lucca.Projeto1.service.SolicitacaoRetiradaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/solicitacoes-retirada")
public class SolicitacaoRetiradaController {
    private final SolicitacaoRetiradaService solicitacaoService;

    public SolicitacaoRetiradaController(SolicitacaoRetiradaService solicitacaoService) {
        this.solicitacaoService = solicitacaoService;
    }

    @PostMapping(value = "/{id}/confirmar", consumes = "multipart/form-data")
    public ResponseEntity<SolicitacaoRetiradaResponse> confirmar(
            @PathVariable Long id,
            @RequestPart("assinatura") MultipartFile assinatura
    ) {
        return ResponseEntity.ok(solicitacaoService.confirmar(id, assinatura));
    }

    @PostMapping
    public ResponseEntity<SolicitacaoRetiradaResponse> criar(
            @Valid @RequestBody SolicitacaoRetiradaRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(solicitacaoService.criar(request, idempotencyKey));
    }

    @GetMapping
    public ResponseEntity<List<SolicitacaoRetiradaResponse>> listar() {
        return ResponseEntity.ok(solicitacaoService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SolicitacaoRetiradaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(solicitacaoService.buscarPorId(id));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<SolicitacaoRetiradaResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(solicitacaoService.cancelar(id));
    }
}
