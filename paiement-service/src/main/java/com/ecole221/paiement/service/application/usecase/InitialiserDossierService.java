package com.ecole221.paiement.service.application.usecase;

import com.ecole221.paiement.service.application.command.InitialiserDossierCommand;
import com.ecole221.paiement.service.application.port.in.InitialiserDossierUseCase;
import com.ecole221.paiement.service.application.port.out.DossierPaiementRepository;
import com.ecole221.paiement.service.application.port.out.PaiementOutboxPort;
import com.ecole221.paiement.service.domain.event.DossierInitialiseEvent;
import com.ecole221.paiement.service.domain.model.DossierPaiement;
import com.ecole221.paiement.service.domain.model.MoyenPaiement;
import com.ecole221.paiement.service.domain.valueobject.TypeMoyen;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;


@Service
@RequiredArgsConstructor
public class InitialiserDossierService implements InitialiserDossierUseCase {

    private final DossierPaiementRepository repository;
    private final PaiementOutboxPort outboxPort;

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

        // Appliquer le versement initial en cascade (valide minimum + plafond)
        MoyenPaiement moyen = buildMoyen(command);
        dossier.distribuerVersement(command.montantVerse(), LocalDate.now(), moyen);

        DossierPaiement saved = repository.sauvegarder(dossier);

        dossier.pullDomainEvents().forEach(event -> {
            if (event instanceof DossierInitialiseEvent e) {
                outboxPort.sauvegarder(e);
            }
        });

        return saved;
    }

    private MoyenPaiement buildMoyen(InitialiserDossierCommand command) {
        return switch (TypeMoyen.valueOf(command.typePaiement())) {
            case MOBILE_MONEY -> MoyenPaiement.mobileMoney(command.operateur(), command.referencePaiement());
            case BANQUE -> MoyenPaiement.banque(command.nomBanque(), command.numeroTransaction());
            case COMPTANT -> MoyenPaiement.comptant();
        };
    }
}
