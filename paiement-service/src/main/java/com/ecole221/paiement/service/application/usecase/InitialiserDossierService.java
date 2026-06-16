package com.ecole221.paiement.service.application.usecase;

import com.ecole221.paiement.service.application.command.InitialiserDossierCommand;
import com.ecole221.paiement.service.application.port.in.InitialiserDossierUseCase;
import com.ecole221.paiement.service.application.port.out.DossierPaiementRepository;
import com.ecole221.paiement.service.domain.model.DossierPaiement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InitialiserDossierService implements InitialiserDossierUseCase {

    private final DossierPaiementRepository repository;

    @Override
    @Transactional
    public DossierPaiement executer(InitialiserDossierCommand command) {
        DossierPaiement dossier = DossierPaiement.initialiser(
                command.inscriptionId(),
                command.etudiantId(),
                command.classeId(),
                command.codeAnnee(),
                command.fraisInscription(),
                command.mensualite(),
                command.autresFrais(),
                command.moisAcademiques()
        );
        return repository.sauvegarder(dossier);
    }
}
