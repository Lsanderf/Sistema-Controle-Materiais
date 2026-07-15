package com.Lucca.Projeto1.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> tratarErrosDeValidacao(
            MethodArgumentNotValidException exception
    ) {

        Map<String, String> camposComErro = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(erro -> camposComErro.put(
                        erro.getField(),
                        erro.getDefaultMessage()
                ));

        Map<String, Object> resposta = new LinkedHashMap<>();

        resposta.put("status", 400);
        resposta.put("erro", "Dados inválidos");
        resposta.put("campos", camposComErro);

        return ResponseEntity
                .badRequest()
                .body(resposta);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> tratarRecursoNaoEncontrado(
            RecursoNaoEncontradoException exception
    ) {
        Map<String, Object> resposta = new LinkedHashMap<>();

        resposta.put("dataHora", dataHoraAtual());
        resposta.put("status", HttpStatus.NOT_FOUND.value());
        resposta.put("erro", exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(resposta);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Map<String, Object>> tratarRegraDeNegocio(
            RegraNegocioException exception
    ) {
        Map<String, Object> resposta = new LinkedHashMap<>();

        resposta.put("dataHora", dataHoraAtual());
        resposta.put("status", HttpStatus.BAD_REQUEST.value());
        resposta.put("erro", exception.getMessage());

        return ResponseEntity
                .badRequest()
                .body(resposta);
    }

    private String dataHoraAtual() {
        return LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        );
    }
}
