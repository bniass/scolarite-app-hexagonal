package com.ecole221.etudiant.service.application.command;

import java.time.LocalDate;

public record CreerEtudiantCommand(
        String matricule,
        String nom,
        String prenom,
        LocalDate dateNaissance
) {}
