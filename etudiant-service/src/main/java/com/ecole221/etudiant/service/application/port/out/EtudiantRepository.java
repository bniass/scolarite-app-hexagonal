package com.ecole221.etudiant.service.application.port.out;

import com.ecole221.etudiant.service.domain.model.Etudiant;

import java.util.Optional;
import java.util.UUID;

public interface EtudiantRepository {
    Etudiant sauvegarder(Etudiant etudiant);
    Optional<Etudiant> trouverParMatricule(String matricule);
    Optional<Etudiant> trouverParId(UUID id);
    boolean existeParMatricule(String matricule);
    boolean existeParEmail(String email);
    long compterParSuffixeAnnee(String suffixeAnnee);
    void supprimerParId(UUID id);
}
