package com.ecole221.inscription.service.application.usecase;

import com.ecole221.inscription.service.application.command.ChangerClasseCommand;
import com.ecole221.inscription.service.application.port.in.ChangerClasseUseCase;
import com.ecole221.inscription.service.application.port.out.InscriptionOutboxPort;
import com.ecole221.inscription.service.application.port.out.InscriptionRepository;
import com.ecole221.inscription.service.application.port.out.SchoolServicePort;
import com.ecole221.inscription.service.application.port.out.TarifActifResult;
import com.ecole221.inscription.service.application.port.out.repository.AnneeAcademiqueProjectionRepository;
import com.ecole221.inscription.service.domain.exception.InscriptionNotFoundException;
import com.ecole221.inscription.service.domain.model.Inscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChangerClasseService implements ChangerClasseUseCase {

    private final InscriptionRepository inscriptionRepository;
    private final InscriptionOutboxPort outboxPort;
    private final SchoolServicePort schoolServicePort;
    private final AnneeAcademiqueProjectionRepository anneeProjectionRepo;

    @Value("${inscription.transfert.delai-max-mois:3}")
    private int delaiMaxMois;

    @Override
    @Transactional
    public Inscription executer(ChangerClasseCommand command) {
        Inscription inscription = inscriptionRepository.trouverParId(command.inscriptionId())
                .orElseThrow(() -> new InscriptionNotFoundException(
                        "Inscription introuvable : " + command.inscriptionId()));

        TarifActifResult tarif = schoolServicePort.getTarifActif(command.nouvelleClasseId());

        var annee = anneeProjectionRepo.findByCodeAnnee(inscription.getCodeAnnee())
                .orElseThrow(() -> new InscriptionNotFoundException(
                        "Projection année introuvable : " + inscription.getCodeAnnee()));

        inscription.changerClasse(
                command.nouvelleClasseId(),
                tarif.fraisInscription(),
                tarif.mensualite(),
                tarif.autresFrais(),
                annee.getMoisAcademiquesJson(),
                tarif.niveau(),
                delaiMaxMois
        );

        Inscription saved = inscriptionRepository.sauvegarder(inscription);
        inscription.pullDomainEvents().forEach(outboxPort::sauvegarder);

        log.info("Inscription {} transférée vers classe {}", command.inscriptionId(), command.nouvelleClasseId());
        return saved;
    }
}
