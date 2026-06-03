package com.ecole221.etudiant.service.application.port.in;

import com.ecole221.etudiant.service.domain.model.Etudiant;

import java.util.UUID;

public interface RechercherEtudiantUseCase {
    Etudiant parMatricule(String matricule);
    Etudiant parId(UUID id);
}
