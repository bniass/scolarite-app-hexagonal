package com.ecole221.paiement.service.infrastructure.web.dto;

import com.ecole221.paiement.service.domain.model.DossierPaiement;
import com.ecole221.paiement.service.domain.model.LignePaiement;
import com.ecole221.paiement.service.domain.valueobject.StatutDossier;
import com.ecole221.paiement.service.domain.valueobject.TypeLigne;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record DossierPaiementResponse(
        UUID id,
        UUID inscriptionId,
        UUID etudiantId,
        UUID classeId,
        String codeAnnee,
        BigDecimal fraisInscription,
        BigDecimal mensualite,
        BigDecimal autresFrais,
        StatutDossier statut,
        List<LignePaiementResponse> lignes
) {
    public static DossierPaiementResponse from(DossierPaiement d) {
        List<LignePaiementResponse> lignes = d.getLignes().stream()
                .sorted(Comparator.comparingInt(DossierPaiementResponse::ordreTri))
                .map(LignePaiementResponse::from)
                .toList();
        return new DossierPaiementResponse(d.getId(), d.getInscriptionId(), d.getEtudiantId(),
                d.getClasseId(), d.getCodeAnnee(), d.getFraisInscription(), d.getMensualite(),
                d.getAutresFrais(), d.getStatut(), lignes);
    }

    private static int ordreTri(LignePaiement l) {
        if (l.getType() == TypeLigne.FRAIS_INSCRIPTION) return -2;
        if (l.getType() == TypeLigne.AUTRES_FRAIS)      return -1;
        return l.getOrdreReglement();
    }
}
