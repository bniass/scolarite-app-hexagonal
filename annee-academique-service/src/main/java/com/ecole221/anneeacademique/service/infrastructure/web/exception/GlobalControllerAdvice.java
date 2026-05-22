package com.ecole221.anneeacademique.service.infrastructure.web.exception;

import com.ecole221.anneeacademique.service.domain.exception.AnneeAcademiqueException;
import com.ecole221.anneeacademique.service.domain.exception.AnneeAcademiqueNotFoundException;
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
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .get(0)
                .getDefaultMessage();

        return ResponseEntity.badRequest().body(
                new ApiError(
                        Instant.now(),
                        400,
                        "Format_invalide",
                        message,
                        request.getRequestURI()
                )
        );
    }
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    @ExceptionHandler(value = {Exception.class})
    public ApiError handleException(Exception exception, HttpServletRequest request){
        return ApiError.builder()
                .timestamp(Instant.now())
                .status(500)
                .error(HttpStatus.INTERNAL_SERVER_ERROR+"")
                .message(exception.getMessage())
                .path(request.getRequestURI())
                .build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest().body(
                new ApiError(
                        Instant.now(),
                        400,
                        "INVALID_ARGUMENT",
                        ex.getMessage(),
                        request.getRequestURI()
                )
        );
    }

    @ExceptionHandler(AnneeAcademiqueNotFoundException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            AnneeAcademiqueNotFoundException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest().body(
                new ApiError(
                        Instant.now(),
                        404,
                        "NOT FOUND",
                        ex.getMessage(),
                        request.getRequestURI()
                )
        );
    }

    @ExceptionHandler(AnneeAcademiqueException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            AnneeAcademiqueException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest().body(
                new ApiError(
                        Instant.now(),
                        400,
                        "EXCEPTION_METIER",
                        ex.getMessage(),
                        request.getRequestURI()
                )
        );
    }
}
