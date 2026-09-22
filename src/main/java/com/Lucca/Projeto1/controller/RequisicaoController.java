package com.Lucca.Projeto1.controller;

import com.Lucca.Projeto1.dto.requisicao.RequisicaoRequest;
import com.Lucca.Projeto1.dto.requisicao.RequisicaoResponse;
import com.Lucca.Projeto1.service.RequisicaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
    public List<RequisicaoResponse> listar() { return requisicaoService.listarAdministrativas(); }

    @GetMapping("/minhas")
    public List<RequisicaoResponse> listarMinhas() { return requisicaoService.listarMinhas(); }

    @GetMapping("/pendentes")
    public List<RequisicaoResponse> listarPendentes() { return requisicaoService.listarPendentes(); }

    @GetMapping("/{id}")
    public RequisicaoResponse buscar(@PathVariable Long id) { return requisicaoService.buscarPorId(id); }

    @PostMapping("/{id}/finalizar-atendimento")
    public RequisicaoResponse finalizar(@PathVariable Long id) {
        return requisicaoService.finalizarAtendimento(id);
    }

    @PostMapping("/{id}/confirmar")
    public RequisicaoResponse confirmar(@PathVariable Long id) {
        return requisicaoService.confirmar(id);
    }
}
