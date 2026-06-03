package com.ecole221.paiement.service.application.command;

import com.ecole221.paiement.service.domain.model.MoyenPaiement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DistribuerVersementCommand(
        UUID inscriptionId,
        BigDecimal montant,
        LocalDate datePaiement,
        MoyenPaiement moyen
) {}
