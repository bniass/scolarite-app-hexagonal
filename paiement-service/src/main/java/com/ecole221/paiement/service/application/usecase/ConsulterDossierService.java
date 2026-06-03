package com.ecole221.paiement.service.application.usecase;

import com.ecole221.paiement.service.application.port.in.ConsulterDossierUseCase;
import com.ecole221.paiement.service.application.port.out.DossierPaiementRepository;
import com.ecole221.paiement.service.domain.exception.PaiementDomainException;
import com.ecole221.paiement.service.domain.model.DossierPaiement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsulterDossierService implements ConsulterDossierUseCase {

    private final DossierPaiementRepository repository;

    @Override
    @Transactional(readOnly = true)
    public DossierPaiement parInscriptionId(UUID inscriptionId) {
        return repository.trouverParInscriptionId(inscriptionId)
                .orElseThrow(() -> new PaiementDomainException(
                        "Dossier introuvable pour inscription : " + inscriptionId));
    }
}
