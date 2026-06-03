package com.ecole221.school.service.infrastructure.web.exception;

import com.ecole221.school.service.domain.exception.SchoolException;
import com.ecole221.school.service.domain.exception.SchoolNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalControllerAdvice {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                      HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(new ApiError(
                Instant.now(), 400, "FORMAT_INVALIDE", message, request.getRequestURI()
        ));
    }

    @ExceptionHandler(SchoolException.class)
    public ResponseEntity<ApiError> handleSchoolException(SchoolException ex,
                                                           HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ApiError(
                Instant.now(), 400, "EXCEPTION_METIER", ex.getMessage(), request.getRequestURI()
        ));
    }

    @ExceptionHandler(SchoolNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(SchoolNotFoundException ex,
                                                    HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(
                Instant.now(), 404, "NOT_FOUND", ex.getMessage(), request.getRequestURI()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex,
                                                           HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ApiError(
                Instant.now(), 400, "INVALID_ARGUMENT", ex.getMessage(), request.getRequestURI()
        ));
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    @ExceptionHandler(Exception.class)
    public ApiError handleException(Exception ex, HttpServletRequest request) {
        return new ApiError(
                Instant.now(), 500, HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                ex.getMessage(), request.getRequestURI()
        );
    }
}
