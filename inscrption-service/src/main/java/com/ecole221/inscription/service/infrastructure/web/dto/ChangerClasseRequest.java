package com.ecole221.inscription.service.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChangerClasseRequest(
        @NotNull(message = "nouvelleClasseId est obligatoire")
        UUID nouvelleClasseId
) {}
