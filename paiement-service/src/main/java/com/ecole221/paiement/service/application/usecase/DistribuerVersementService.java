package com.ecole221.paiement.service.application.usecase;

import com.ecole221.paiement.service.application.command.DistribuerVersementCommand;
import com.ecole221.paiement.service.application.port.in.DistribuerVersementUseCase;
import com.ecole221.paiement.service.application.port.out.DossierPaiementRepository;
import com.ecole221.paiement.service.application.port.out.PaiementOutboxPort;
import com.ecole221.paiement.service.domain.event.DossierInitialiseEvent;
import com.ecole221.paiement.service.domain.exception.PaiementDomainException;
import com.ecole221.paiement.service.domain.model.DossierPaiement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DistribuerVersementService implements DistribuerVersementUseCase {

    private final DossierPaiementRepository repository;
    private final PaiementOutboxPort outboxPort;

    @Override
    @Transactional
    public DossierPaiement executer(DistribuerVersementCommand command) {
        DossierPaiement dossier = repository.trouverParInscriptionId(command.inscriptionId())
                .orElseThrow(() -> new PaiementDomainException(
                        "Dossier introuvable pour inscription : " + command.inscriptionId()));

        dossier.distribuerVersement(command.montant(), command.datePaiement(), command.moyen());

        DossierPaiement saved = repository.sauvegarder(dossier);

        // Premier versement (INITIALISE→ACTIF) → confirmer l'inscription via outbox
        dossier.pullDomainEvents().forEach(event -> {
            if (event instanceof DossierInitialiseEvent e) {
                outboxPort.sauvegarder(e);
            }
        });

        return saved;
    }
}
