package com.ecole221.inscription.service.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreerInscriptionRequest(
        UUID etudiantId,
        @Valid NouvelEtudiantRequest nouvelEtudiant,
        @NotNull UUID classeId,
        @NotBlank String codeAnnee,
        @NotNull @Positive BigDecimal montant,
        @NotNull @Valid MoyenPaiementRequest moyenPaiement
) {
    /**
     * Exactement l'un des deux doit être fourni :
     * soit un étudiant existant (etudiantId), soit un nouvel étudiant (nouvelEtudiant).
     */
    @AssertTrue(message = "Fournissez soit un etudiantId (étudiant existant) soit les informations d'un nouvel étudiant, pas les deux ni aucun")
    public boolean isEtudiantValide() {
        return (etudiantId != null) ^ (nouvelEtudiant != null);
    }
}
