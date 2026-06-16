package com.ecole221.paiement.service.application.query;

import com.ecole221.paiement.service.domain.valueobject.StatutDossier;

import java.math.BigDecimal;
import java.util.UUID;

public record DossierResume(
        UUID dossierId,
        UUID inscriptionId,
        UUID etudiantId,
        UUID classeId,
        String codeAnnee,
        StatutDossier statut,
        BigDecimal montantTotalDu,
        BigDecimal montantTotalPaye,
        BigDecimal montantTotalRestant
) {}
