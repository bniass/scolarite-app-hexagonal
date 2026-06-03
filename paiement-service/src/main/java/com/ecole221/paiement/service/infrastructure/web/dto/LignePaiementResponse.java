package com.ecole221.paiement.service.infrastructure.web.dto;

import com.ecole221.paiement.service.domain.model.LignePaiement;
import com.ecole221.paiement.service.domain.model.MoisAcademique;
import com.ecole221.paiement.service.domain.valueobject.StatutLigne;
import com.ecole221.paiement.service.domain.valueobject.TypeLigne;

import java.math.BigDecimal;
import java.util.UUID;

public record LignePaiementResponse(
        UUID id,
        TypeLigne type,
        MoisAcademique moisAcademique,
        int ordreReglement,
        BigDecimal montantDu,
        BigDecimal montantPaye,
        BigDecimal montantRestant,
        String commentaire,
        StatutLigne statut
) {
    public static LignePaiementResponse from(LignePaiement l) {
        return new LignePaiementResponse(l.getId(), l.getType(), l.getMoisAcademique(),
                l.getOrdreReglement(), l.getMontantDu(), l.getMontantPaye(),
                l.getMontantRestant(), l.getCommentaire(), l.getStatut());
    }
}
