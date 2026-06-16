package com.ecole221.paiement.service.application.usecase;

import com.ecole221.paiement.service.application.command.InitialiserDossierCommand;
import com.ecole221.paiement.service.application.port.in.InitialiserDossierUseCase;
import com.ecole221.paiement.service.application.port.in.TransfererDossierUseCase;
import com.ecole221.paiement.service.application.port.out.DossierPaiementRepository;
import com.ecole221.paiement.service.domain.model.DossierPaiement;
import com.ecole221.paiement.service.domain.model.LignePaiement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransfererDossierService implements TransfererDossierUseCase {

    private final DossierPaiementRepository repository;
    private final InitialiserDossierUseCase initialiserDossierUseCase;

    @Override
    @Transactional
    public void executer(InitialiserDossierCommand command) {
        // 1. Capturer le total déjà payé sur l'ancien dossier avant suppression
        BigDecimal totalPaye = repository.trouverParInscriptionId(command.inscriptionId())
                .map(ancien -> ancien.getLignes().stream()
                        .map(LignePaiement::getMontantPaye)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .orElse(BigDecimal.ZERO);

        log.info("Ancien dossier : {} FCFA payés pour inscription {} — sera redistribué sur la nouvelle classe {}",
                totalPaye, command.inscriptionId(), command.classeId());

        // 2. Supprimer l'ancien dossier (cascade : lignes, versements, moyens)
        repository.supprimerParInscriptionId(command.inscriptionId());

        // 3. Initialiser le nouveau dossier avec les tarifs de la nouvelle classe
        DossierPaiement nouveauDossier = initialiserDossierUseCase.executer(command);

        // 4. Redistribuer intégralement le montant déjà payé sur le nouveau dossier
        if (totalPaye.compareTo(BigDecimal.ZERO) > 0) {
            nouveauDossier.appliquerTransfert(totalPaye, LocalDate.now());
            repository.sauvegarder(nouveauDossier);
            log.info("Redistribution de {} FCFA sur nouveau dossier inscription {}", totalPaye, command.inscriptionId());
        }
    }
}
