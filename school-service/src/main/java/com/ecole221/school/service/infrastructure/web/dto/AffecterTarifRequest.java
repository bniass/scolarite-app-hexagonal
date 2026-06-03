package com.ecole221.school.service.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AffecterTarifRequest(
        @NotNull(message = "L'identifiant du tarif est obligatoire") UUID tarifId
) {}
