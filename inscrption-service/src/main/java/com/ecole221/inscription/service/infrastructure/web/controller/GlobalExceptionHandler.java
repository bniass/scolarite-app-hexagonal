package com.ecole221.inscription.service.infrastructure.web.controller;

import com.ecole221.inscription.service.domain.exception.InscriptionException;
import com.ecole221.inscription.service.domain.exception.InscriptionNotFoundException;
import com.ecole221.inscription.service.domain.exception.SchoolServiceIndisponibleException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InscriptionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(InscriptionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(404, "NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InscriptionException.class)
    public ResponseEntity<Map<String, Object>> handleDomain(InscriptionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(400, "BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(SchoolServiceIndisponibleException.class)
    public ResponseEntity<Map<String, Object>> handleSchoolServiceIndisponible(SchoolServiceIndisponibleException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(error(503, "SERVICE_UNAVAILABLE", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " : " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(400, "VALIDATION_ERROR", details));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadable(HttpMessageNotReadableException ex) {
        String message = "Corps de la requête invalide : " + ex.getMostSpecificCause().getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(400, "BAD_REQUEST", message));
    }

    private Map<String, Object> error(int status, String error, String message) {
        return Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status,
                "error", error,
                "message", message
        );
    }
}
