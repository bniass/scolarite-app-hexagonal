package com.ecole221.etudiant.service.infrastructure.web.exception;

import com.ecole221.etudiant.service.domain.exception.EtudiantException;
import com.ecole221.etudiant.service.domain.exception.EtudiantNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalControllerAdvice {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                      HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(
                new ApiError(Instant.now(), 400, "VALIDATION_ERROR", message, request.getRequestURI()));
    }

    @ExceptionHandler(EtudiantNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(EtudiantNotFoundException ex,
                                                    HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ApiError(Instant.now(), 404, "NOT_FOUND", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(EtudiantException.class)
    public ResponseEntity<ApiError> handleMetier(EtudiantException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(
                new ApiError(Instant.now(), 400, "ERREUR_METIER", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiError(Instant.now(), 500, "INTERNAL_SERVER_ERROR", ex.getMessage(), request.getRequestURI()));
    }
}
