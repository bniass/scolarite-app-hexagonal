package com.ecole221.inscription.service.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreerInscriptionRequest(
        UUID etudiantId,
        @Valid NouvelEtudiantRequest nouvelEtudiant,
        @NotNull UUID classeId,
        @NotBlank String codeAnnee
) {
    @AssertTrue(message = "Fournissez soit un etudiantId (étudiant existant) soit les informations d'un nouvel étudiant, pas les deux ni aucun")
    @JsonIgnore
    public boolean isEtudiantValide() {
        return (etudiantId != null) ^ (nouvelEtudiant != null);
    }
}
