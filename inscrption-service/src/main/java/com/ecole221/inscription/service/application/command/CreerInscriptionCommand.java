package com.ecole221.inscription.service.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreerInscriptionCommand(
        // Étudiant existant (null si nouvel étudiant)
        UUID etudiantId,
        // Informations d'un nouvel étudiant (null si étudiant existant)
        String nomEtudiant,
        String prenomEtudiant,
        LocalDate dateNaissanceEtudiant,
        String emailEtudiant,
        // Données de l'inscription
        UUID classeId,
        String codeAnnee,
        BigDecimal montant,
        String typePaiement,
        String operateur,
        String referencePaiement,
        String nomBanque,
        String numeroTransaction
) {}
