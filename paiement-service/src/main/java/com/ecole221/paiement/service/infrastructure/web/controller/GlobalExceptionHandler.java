package com.ecole221.paiement.service.infrastructure.web.controller;

import com.ecole221.paiement.service.domain.exception.PaiementDomainException;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
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

    @ExceptionHandler(PaiementDomainException.class)
    public ResponseEntity<Map<String, Object>> handleDomain(PaiementDomainException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(400, "BAD_REQUEST", ex.getMessage()));
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
        String message;
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidTypeIdException ite) {
            message = "Valeur de type inconnue : \"" + ite.getTypeId()
                    + "\". Valeurs acceptées : MOBILE_MONEY, BANQUE, COMPTANT";
        } else {
            message = "Corps de la requête invalide ou mal formé";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(400, "BAD_REQUEST", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(500, "INTERNAL_SERVER_ERROR", "Une erreur interne est survenue"));
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
