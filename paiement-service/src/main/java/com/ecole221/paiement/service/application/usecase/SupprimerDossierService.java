package com.ecole221.paiement.service.application.usecase;

import com.ecole221.paiement.service.application.port.in.SupprimerDossierUseCase;
import com.ecole221.paiement.service.application.port.out.DossierPaiementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupprimerDossierService implements SupprimerDossierUseCase {

    private final DossierPaiementRepository repository;

    @Override
    @Transactional
    public void supprimerParInscriptionId(UUID inscriptionId) {
        repository.supprimerParInscriptionId(inscriptionId);
        log.info("Dossier paiement supprimé physiquement pour inscription {}", inscriptionId);
    }
}
