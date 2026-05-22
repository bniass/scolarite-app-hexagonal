package com.ecole221.etudiant.service.domain.exception;

public class EtudiantNotFoundException extends RuntimeException {
    public EtudiantNotFoundException(String matricule) {
        super("Étudiant introuvable pour le matricule : " + matricule);
    }
}
