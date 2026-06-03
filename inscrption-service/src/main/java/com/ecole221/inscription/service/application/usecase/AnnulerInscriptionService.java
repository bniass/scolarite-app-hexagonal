package com.ecole221.inscription.service.application.usecase;

import com.ecole221.inscription.service.application.port.in.AnnulerInscriptionUseCase;
import com.ecole221.inscription.service.application.port.out.EtudiantServicePort;
import com.ecole221.inscription.service.application.port.out.InscriptionRepository;
import com.ecole221.inscription.service.domain.exception.InscriptionNotFoundException;
import com.ecole221.inscription.service.domain.model.Inscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnnulerInscriptionService implements AnnulerInscriptionUseCase {

    private final InscriptionRepository inscriptionRepository;
    private final EtudiantServicePort etudiantServicePort;

    @Override
    @Transactional
    public void executer(UUID inscriptionId, String motif) {
        Inscription inscription = inscriptionRepository.trouverParId(inscriptionId)
                .orElseThrow(() -> new InscriptionNotFoundException(
                        "Inscription introuvable : " + inscriptionId));

        // Supprimer physiquement l'inscription (saga compensation = rollback complet)
        inscriptionRepository.supprimer(inscriptionId);
        log.info("Inscription {} supprimée (compensation saga, motif: {})", inscriptionId, motif);

        // Ne supprimer l'étudiant que s'il a été créé lors de cette inscription
        // (si l'étudiant existait avant, sa suppression serait une destruction de données)
        if (inscription.isEtudiantNouveau()) {
            try {
                etudiantServicePort.supprimerEtudiant(inscription.getEtudiantId());
                log.info("Étudiant {} supprimé (compensation inscription {})",
                        inscription.getEtudiantId(), inscriptionId);
            } catch (Exception e) {
                log.error("Compensation échouée: impossible de supprimer l'étudiant {} : {}",
                        inscription.getEtudiantId(), e.getMessage());
            }
        } else {
            log.info("Étudiant {} conservé (étudiant existant, pas de compensation)",
                    inscription.getEtudiantId());
        }
    }
}
