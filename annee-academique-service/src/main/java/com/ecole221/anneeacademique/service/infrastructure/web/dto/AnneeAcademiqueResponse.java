package com.ecole221.anneeacademique.service.infrastructure.web.dto;

import com.ecole221.anneeacademique.service.domain.model.AnneeAcademique;

import java.time.LocalDate;

public record AnneeAcademiqueResponse(
        String code,
        LocalDate dateDebut,
        LocalDate dateFin,
        LocalDate dateDebutInscriptions,
        LocalDate dateFinInscriptions,
        LocalDate datePublication,
        String statut
) {
    public static AnneeAcademiqueResponse from(AnneeAcademique annee) {
        return new AnneeAcademiqueResponse(
                String.valueOf(annee.getId().getValue().getCodeAnnee()),
                annee.getDateDebut(),
                annee.getDateFin(),
                annee.getDateOuvertureInscription(),
                annee.getDateFinInscription(),
                annee.getDatePublication(),
                annee.getEtatAnnee().getClass().getSimpleName()
        );
    }
}
