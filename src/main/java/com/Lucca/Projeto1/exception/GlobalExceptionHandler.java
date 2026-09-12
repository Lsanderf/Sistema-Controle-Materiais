package com.Lucca.Projeto1.exception;

import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

        Map<String, Object> resposta = respostaBase(
                HttpStatus.BAD_REQUEST,
                "Dados inválidos"
        );
        resposta.put("campos", camposComErro);

        return ResponseEntity
                .badRequest()
                .body(resposta);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> tratarConstraintViolation(
            ConstraintViolationException exception
    ) {
        Map<String, String> camposComErro = new LinkedHashMap<>();

        exception.getConstraintViolations()
                .forEach(violacao -> camposComErro.put(
                        violacao.getPropertyPath().toString(),
                        violacao.getMessage()
                ));

        Map<String, Object> resposta = respostaBase(
                HttpStatus.BAD_REQUEST,
                "Dados inválidos"
        );
        resposta.put("campos", camposComErro);

        return ResponseEntity
                .badRequest()
                .body(resposta);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> tratarMensagemNaoLida(
            HttpMessageNotReadableException exception
    ) {
        return responder(
                HttpStatus.BAD_REQUEST,
                "JSON malformado ou campo com valor inválido"
        );
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> tratarRecursoNaoEncontrado(
            RecursoNaoEncontradoException exception
    ) {
        return responder(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> tratarRotaNaoEncontrada(
            NoResourceFoundException exception
    ) {
        return responder(HttpStatus.NOT_FOUND, "Recurso nÃ£o encontrado");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> tratarMetodoNaoSuportado(
            HttpRequestMethodNotSupportedException exception
    ) {
        return responder(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Metodo HTTP nao permitido para este recurso"
        );
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> tratarParteMultipartAusente(
            MissingServletRequestPartException exception
    ) {
        return responder(
                HttpStatus.BAD_REQUEST,
                switch (exception.getRequestPartName()) {
                    case "movimentacao" -> "Os dados da movimentação são obrigatórios";
                    case "assinatura" -> "O arquivo da assinatura é obrigatório";
                    default -> "A parte '" + exception.getRequestPartName() + "' é obrigatória";
                }
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> tratarArquivoMuitoGrande(
            MaxUploadSizeExceededException exception,
            HttpServletRequest request
    ) {
        String mensagem = request.getRequestURI()
                .startsWith("/notas-fiscais/importar-xml")
                ? "O arquivo XML da NF-e excede o tamanho máximo permitido"
                : "Os arquivos de evidência excedem o tamanho máximo permitido";
        return responder(
                HttpStatus.PAYLOAD_TOO_LARGE,
                mensagem
        );
    }

    @ExceptionHandler(ArquivoMuitoGrandeException.class)
    public ResponseEntity<Map<String, Object>> tratarArquivoMuitoGrande(
            ArquivoMuitoGrandeException exception
    ) {
        return responder(HttpStatus.PAYLOAD_TOO_LARGE, exception.getMessage());
    }

    @ExceptionHandler(XmlNfeInvalidoException.class)
    public ResponseEntity<Map<String, Object>> tratarXmlNfeInvalido(
            XmlNfeInvalidoException exception
    ) {
        return responder(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Map<String, Object>> tratarRegraDeNegocio(
            RegraNegocioException exception
    ) {
        return responder(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<Map<String, Object>> tratarCredenciaisInvalidas(
            CredenciaisInvalidasException exception
    ) {
        return responder(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> tratarViolacaoDeIntegridade(
            DataIntegrityViolationException exception
    ) {
        return responder(
                HttpStatus.CONFLICT,
                "A operação viola uma restrição de integridade dos dados"
        );
    }

    @ExceptionHandler({
            CannotAcquireLockException.class,
            LockTimeoutException.class,
            OptimisticLockException.class,
            PessimisticLockException.class,
            PessimisticLockingFailureException.class,
            jakarta.persistence.QueryTimeoutException.class,
            QueryTimeoutException.class,
            TransactionTimedOutException.class
    })
    public ResponseEntity<Map<String, Object>> tratarConcorrencia(
            Exception exception
    ) {
        return responder(
                HttpStatus.CONFLICT,
                "Não foi possível concluir a operação por concorrência. Tente novamente"
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> tratarErroInesperado(
            Exception exception
    ) {
        return responder(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro interno inesperado"
        );
    }

    private ResponseEntity<Map<String, Object>> responder(
            HttpStatus status,
            String erro
    ) {
        return ResponseEntity
                .status(status)
                .body(respostaBase(status, erro));
    }

    private Map<String, Object> respostaBase(HttpStatus status, String erro) {
        Map<String, Object> resposta = new LinkedHashMap<>();

        resposta.put("dataHora", dataHoraAtual());
        resposta.put("status", status.value());
        resposta.put("erro", erro);

        return resposta;
    }

    private String dataHoraAtual() {
        return LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        );
    }
}
