package com.Lucca.Projeto1.controller;

import com.Lucca.Projeto1.dto.usuario.UsuarioAtualizacaoRequest;
import com.Lucca.Projeto1.dto.usuario.UsuarioRequest;
import com.Lucca.Projeto1.dto.usuario.UsuarioResponse;
import com.Lucca.Projeto1.dto.usuario.EncarregadoRequest;
import com.Lucca.Projeto1.dto.usuario.EncarregadoResumoResponse;
import com.Lucca.Projeto1.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @GetMapping("/encarregados")
    public ResponseEntity<List<EncarregadoResumoResponse>> listarEncarregados() {
        return ResponseEntity.ok(usuarioService.listarEncarregadosAtivos());
    }

    @PostMapping("/encarregados")
    public ResponseEntity<EncarregadoResumoResponse> cadastrarEncarregado(
            @Valid @RequestBody EncarregadoRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(usuarioService.cadastrarEncarregado(request));
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> cadastrar(
            @Valid @RequestBody UsuarioRequest request
    ) {
        UsuarioResponse usuario = usuarioService.cadastrar(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(usuario);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioAtualizacaoRequest request
    ) {
        return ResponseEntity.ok(
                usuarioService.atualizar(id, request)
        );
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<UsuarioResponse> ativar(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(usuarioService.ativar(id));
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<UsuarioResponse> desativar(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                usuarioService.desativar(id, authentication.getName())
        );
    }
}
