package com.ecole221.school.service.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreerTarifRequest(
        @NotNull @PositiveOrZero BigDecimal fraisInscription,
        @NotNull @PositiveOrZero BigDecimal mensualite,
        @NotNull @PositiveOrZero BigDecimal autresFrais
) {}
