package com.ecole221.paiement.service.application.query;

import java.math.BigDecimal;

public record ResumeTableauBord(
        String codeAnnee,
        long totalDossiers,
        long dossiersInitialises,
        long dossiersActifs,
        long dossiersClotures,
        BigDecimal montantTotalDu,
        BigDecimal montantTotalPaye,
        BigDecimal montantTotalRestant,
        long nombreLignesImpayees
) {}
