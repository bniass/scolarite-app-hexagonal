package com.ecole221.inscription.service.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record CreerInscriptionCommand(
        UUID etudiantId,
        String nomEtudiant,
        String prenomEtudiant,
        LocalDate dateNaissanceEtudiant,
        String emailEtudiant,
        UUID classeId,
        String codeAnnee
) {}
