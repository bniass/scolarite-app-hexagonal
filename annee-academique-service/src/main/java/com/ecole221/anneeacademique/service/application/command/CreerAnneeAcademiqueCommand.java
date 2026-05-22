package com.ecole221.anneeacademique.service.application.command;

import java.time.LocalDate;

public record CreerAnneeAcademiqueCommand(
        int codeAnnee,
        LocalDate dateDebut,
        LocalDate dateFin,
        LocalDate dateOuvertureInscription,
        LocalDate dateFinInscription
) {}
