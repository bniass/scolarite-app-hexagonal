package com.ecole221.etudiant.service.application.port.out;

import com.ecole221.etudiant.service.domain.model.Etudiant;

import java.util.Optional;

public interface EtudiantRepository {
    Etudiant sauvegarder(Etudiant etudiant);
    Optional<Etudiant> trouverParMatricule(String matricule);
    boolean existeParMatricule(String matricule);
}
