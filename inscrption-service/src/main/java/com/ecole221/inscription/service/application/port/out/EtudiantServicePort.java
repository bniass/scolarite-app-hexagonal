package com.ecole221.inscription.service.application.port.out;

import java.time.LocalDate;
import java.util.UUID;

public interface EtudiantServicePort {
    void supprimerEtudiant(UUID etudiantId);
    UUID creerEtudiant(String nom, String prenom, LocalDate dateNaissance, String email, String codeAnnee);
}
