package com.ecole221.school.service.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record HistoriqueTarifResponse(
        UUID tarifId,
        BigDecimal fraisInscription,
        BigDecimal mensualite,
        BigDecimal autresFrais,
        LocalDate dateActivation,
        LocalDate dateDesactivation,
        boolean actif
) {}
