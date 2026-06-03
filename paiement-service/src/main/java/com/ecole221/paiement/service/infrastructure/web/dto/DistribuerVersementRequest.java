package com.ecole221.paiement.service.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DistribuerVersementRequest(
        @NotNull @Positive BigDecimal montant,
        @NotNull LocalDate datePaiement,
        @NotNull MoyenPaiementRequest moyen
) {}
