package com.Lucca.Projeto1.controller;

import com.Lucca.Projeto1.dto.requisicao.RequisicaoRequest;
import com.Lucca.Projeto1.dto.requisicao.RequisicaoResponse;
import com.Lucca.Projeto1.service.RequisicaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/requisicoes")
public class RequisicaoController {
    private final RequisicaoService requisicaoService;

    public RequisicaoController(RequisicaoService requisicaoService) {
        this.requisicaoService = requisicaoService;
    }

    @PostMapping
    public ResponseEntity<RequisicaoResponse> criar(@Valid @RequestBody RequisicaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(requisicaoService.criar(request));
    }

    @GetMapping
    public ResponseEntity<List<RequisicaoResponse>> listar() {
        return ResponseEntity.ok(requisicaoService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RequisicaoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(requisicaoService.buscarPorId(id));
    }

    @PatchMapping("/{id}/visualizar")
    public ResponseEntity<RequisicaoResponse> visualizar(@PathVariable Long id) {
        return ResponseEntity.ok(requisicaoService.visualizar(id));
    }

    @PatchMapping("/{id}/concluir")
    public ResponseEntity<RequisicaoResponse> concluir(@PathVariable Long id) {
        return ResponseEntity.ok(requisicaoService.concluir(id));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<RequisicaoResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(requisicaoService.cancelar(id));
    }
}
