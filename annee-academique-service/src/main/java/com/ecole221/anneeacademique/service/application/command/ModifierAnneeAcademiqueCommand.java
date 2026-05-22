package com.ecole221.anneeacademique.service.application.command;

import java.time.LocalDate;

public record ModifierAnneeAcademiqueCommand(
        int code,
        LocalDate dateDebut,
        LocalDate dateFin,
        LocalDate dateDebutInscriptions,
        LocalDate dateFinInscriptions
) {}
