package com.ecole221.school.service.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record HistoriqueTarifItem(
        UUID tarifId,
        BigDecimal fraisInscription,
        BigDecimal mensualite,
        BigDecimal autresFrais,
        LocalDate dateActivation,
        LocalDate dateDesactivation,
        boolean actif
) {}
