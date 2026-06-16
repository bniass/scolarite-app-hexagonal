package com.ecole221.paiement.service.application.query;

import com.ecole221.paiement.service.domain.valueobject.StatutLigne;
import com.ecole221.paiement.service.domain.valueobject.TypeLigne;

import java.math.BigDecimal;
import java.util.UUID;

public record LigneImpayee(
        UUID ligneId,
        UUID inscriptionId,
        UUID etudiantId,
        UUID classeId,
        String codeAnnee,
        TypeLigne type,
        String libelle,
        BigDecimal montantDu,
        BigDecimal montantPaye,
        BigDecimal montantRestant,
        StatutLigne statut
) {}
