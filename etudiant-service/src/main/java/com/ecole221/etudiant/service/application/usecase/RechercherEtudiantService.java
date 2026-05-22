package com.ecole221.etudiant.service.application.usecase;

import com.ecole221.etudiant.service.application.port.in.RechercherEtudiantUseCase;
import com.ecole221.etudiant.service.application.port.out.EtudiantRepository;
import com.ecole221.etudiant.service.domain.exception.EtudiantNotFoundException;
import com.ecole221.etudiant.service.domain.model.Etudiant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RechercherEtudiantService implements RechercherEtudiantUseCase {

    private final EtudiantRepository etudiantRepository;

    public RechercherEtudiantService(EtudiantRepository etudiantRepository) {
        this.etudiantRepository = etudiantRepository;
    }

    @Override
    public Etudiant parMatricule(String matricule) {
        return etudiantRepository.trouverParMatricule(matricule)
                .orElseThrow(() -> new EtudiantNotFoundException(matricule));
    }
}
