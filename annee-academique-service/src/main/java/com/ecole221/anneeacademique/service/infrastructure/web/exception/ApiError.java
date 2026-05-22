package com.ecole221.anneeacademique.service.infrastructure.web.exception;

import lombok.Builder;

import java.time.Instant;

@Builder
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {}
