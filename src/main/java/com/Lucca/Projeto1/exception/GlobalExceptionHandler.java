package com.Lucca.Projeto1.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
}
