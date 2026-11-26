package br.com.ifba.academia.usuario.controller;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ManipuladorExcecoesApi {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> tratarErroHttp(ResponseStatusException exception) {
        String message = exception.getReason() == null ? "Solicitação inválida." : exception.getReason();
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of("mensagem", message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> tratarRequisicaoInvalida(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("mensagem", mensagemSegura(exception)));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> tratarErroDominio(RuntimeException exception) {
        String message = mensagemSegura(exception);
        String normalized = message.toLowerCase();
        HttpStatus status = normalized.contains("nao encontrado") || normalized.contains("não encontrado")
                ? HttpStatus.NOT_FOUND
                : normalized.contains("ja existe") || normalized.contains("já existe")
                        ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(Map.of("mensagem", message));
    }

    private String mensagemSegura(Exception exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? "Não foi possível processar a solicitação." : exception.getMessage();
    }
}

