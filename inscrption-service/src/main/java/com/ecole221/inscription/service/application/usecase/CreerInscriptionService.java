package com.ecole221.inscription.service.application.usecase;

import com.ecole221.inscription.service.application.command.CreerInscriptionCommand;
import com.ecole221.inscription.service.application.port.in.CreerInscriptionUseCase;
import com.ecole221.inscription.service.application.port.out.*;
import com.ecole221.inscription.service.application.port.out.repository.AnneeAcademiqueProjectionRepository;
import com.ecole221.inscription.service.domain.event.InscriptionCreeeEvent;
import com.ecole221.inscription.service.domain.exception.InscriptionException;
import com.ecole221.inscription.service.domain.model.Inscription;
import com.ecole221.inscription.service.domain.valueobject.EtatAnnee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreerInscriptionService implements CreerInscriptionUseCase {

    private final InscriptionRepository inscriptionRepository;
    private final InscriptionOutboxPort outboxPort;
    private final AnneeAcademiqueProjectionRepository anneeProjectionRepo;
    private final SchoolServicePort schoolServicePort;
    private final EtudiantServicePort etudiantServicePort;

    @Override
    @Transactional
    public Inscription executer(CreerInscriptionCommand command) {
        var projection = anneeProjectionRepo.findByCodeAnnee(command.codeAnnee())
                .orElseThrow(() -> new InscriptionException(
                        "Année académique introuvable : " + command.codeAnnee()));

        if (projection.getEtatAnnee() != EtatAnnee.INSCRIPTIONS_OUVERTES) {
            throw new InscriptionException(
                    "Les inscriptions ne sont pas ouvertes pour l'année " + command.codeAnnee());
        }

        var tarif = schoolServicePort.getTarifActif(command.classeId());

        boolean etudiantNouveau = (command.etudiantId() == null);
        UUID etudiantId = resolveEtudiantId(command);

        Inscription inscription = Inscription.creer(
                etudiantId,
                etudiantNouveau,
                command.classeId(),
                command.codeAnnee(),
                tarif.fraisInscription(),
                tarif.mensualite(),
                tarif.autresFrais(),
                projection.getMoisAcademiquesJson()
        );

        Inscription saved = inscriptionRepository.sauvegarder(inscription);

        inscription.pullDomainEvents().forEach(event -> {
            if (event instanceof InscriptionCreeeEvent e) {
                outboxPort.sauvegarder(e);
            }
        });

        return saved;
    }

    private UUID resolveEtudiantId(CreerInscriptionCommand command) {
        if (command.etudiantId() != null) {
            return command.etudiantId();
        }
        return etudiantServicePort.creerEtudiant(
                command.nomEtudiant(),
                command.prenomEtudiant(),
                command.dateNaissanceEtudiant(),
                command.emailEtudiant(),
                command.codeAnnee()
        );
    }
}
