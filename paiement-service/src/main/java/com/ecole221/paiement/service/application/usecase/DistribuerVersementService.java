package com.ecole221.paiement.service.application.usecase;

import com.ecole221.paiement.service.application.command.DistribuerVersementCommand;
import com.ecole221.paiement.service.application.port.in.DistribuerVersementUseCase;
import com.ecole221.paiement.service.application.port.out.DossierPaiementRepository;
import com.ecole221.paiement.service.domain.exception.PaiementDomainException;
import com.ecole221.paiement.service.domain.model.DossierPaiement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DistribuerVersementService implements DistribuerVersementUseCase {

    private final DossierPaiementRepository repository;

    @Override
    @Transactional
    public DossierPaiement executer(DistribuerVersementCommand command) {
        DossierPaiement dossier = repository.trouverParInscriptionId(command.inscriptionId())
                .orElseThrow(() -> new PaiementDomainException(
                        "Dossier introuvable pour inscription : " + command.inscriptionId()));

        dossier.distribuerVersement(command.montant(), command.datePaiement(), command.moyen());

        return repository.sauvegarder(dossier);
    }
}
