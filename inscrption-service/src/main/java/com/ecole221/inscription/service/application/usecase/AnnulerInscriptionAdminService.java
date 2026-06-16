package com.ecole221.inscription.service.application.usecase;

import com.ecole221.inscription.service.application.port.in.AnnulerInscriptionAdminUseCase;
import com.ecole221.inscription.service.application.port.out.InscriptionOutboxPort;
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
public class AnnulerInscriptionAdminService implements AnnulerInscriptionAdminUseCase {

    private final InscriptionRepository inscriptionRepository;
    private final InscriptionOutboxPort outboxPort;

    @Override
    @Transactional
    public void executer(UUID inscriptionId, String motif) {
        Inscription inscription = inscriptionRepository.trouverParId(inscriptionId)
                .orElseThrow(() -> new InscriptionNotFoundException(
                        "Inscription introuvable : " + inscriptionId));

        inscription.annuler(motif);
        inscriptionRepository.sauvegarder(inscription);

        inscription.pullDomainEvents().forEach(outboxPort::sauvegarder);

        log.info("Inscription {} annulée (motif: {})", inscriptionId, motif);
    }
}
