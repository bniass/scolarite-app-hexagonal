package com.ecole221.etudiant.service.application.usecase;

import com.ecole221.etudiant.service.application.port.in.SupprimerEtudiantUseCase;
import com.ecole221.etudiant.service.application.port.out.EtudiantRepository;
import com.ecole221.etudiant.service.domain.exception.EtudiantNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class SupprimerEtudiantService implements SupprimerEtudiantUseCase {

    private final EtudiantRepository etudiantRepository;

    public SupprimerEtudiantService(EtudiantRepository etudiantRepository) {
        this.etudiantRepository = etudiantRepository;
    }

    @Override
    public void executer(UUID id) {
        if (etudiantRepository.trouverParId(id).isEmpty()) {
            throw new EtudiantNotFoundException("Étudiant introuvable avec l'id : " + id);
        }
        etudiantRepository.supprimerParId(id);
    }
}
