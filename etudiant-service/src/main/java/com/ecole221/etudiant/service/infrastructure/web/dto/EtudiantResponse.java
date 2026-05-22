package com.ecole221.etudiant.service.infrastructure.web.dto;

import java.time.LocalDate;
import java.util.UUID;

public record EtudiantResponse(
        UUID id,
        String matricule,
        String nom,
        String prenom,
        LocalDate dateNaissance
) {}
