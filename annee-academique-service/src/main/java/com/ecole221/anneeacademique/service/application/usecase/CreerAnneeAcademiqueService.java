package com.ecole221.anneeacademique.service.application.usecase;


import com.ecole221.anneeacademique.service.application.command.CreerAnneeAcademiqueCommand;
import com.ecole221.anneeacademique.service.application.port.in.CreerAnneeAcademiqueUseCase;
import com.ecole221.anneeacademique.service.application.port.out.outbox.OutboxPort;
import com.ecole221.anneeacademique.service.application.port.out.repository.AnneeAcademiqueRepository;
import com.ecole221.anneeacademique.service.domain.model.AnneeAcademique;
import com.ecole221.anneeacademique.service.domain.model.DatesAnnee;
import com.ecole221.anneeacademique.service.domain.exception.AnneeAcademiqueException;
import com.ecole221.anneeacademique.service.domain.model.Statut;
import com.ecole221.anneeacademique.service.domain.valuobject.CodeAnnee;
import com.ecole221.common.event.publisher.DomainEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class CreerAnneeAcademiqueService
        implements CreerAnneeAcademiqueUseCase {

    private final AnneeAcademiqueRepository repository;
    private final DomainEventPublisher eventPublisher;
    private final OutboxPort outboxPort;

    public CreerAnneeAcademiqueService(
            AnneeAcademiqueRepository repository, DomainEventPublisher eventPublisher, OutboxPort outboxPort
    ) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.outboxPort = outboxPort;
    }

    @Transactional
    @Override
    public void executer(CreerAnneeAcademiqueCommand cmd) {

        if (repository.findByCode(new CodeAnnee(cmd.codeAnnee()).getCodeAnnee()).isPresent()) {
            throw new AnneeAcademiqueException(
                    "Année académique déjà existante"
            );
        }

        if(repository.existsByStatutNot(Statut.CLOTUREE)){
            throw new AnneeAcademiqueException(
                    "Impossible de creer cette année académique tant qu'il existe une annee non cloturee!"
            );
        }

        DatesAnnee datesAnnee = new DatesAnnee(
                cmd.dateDebut(),
                cmd.dateFin(),
                cmd.dateOuvertureInscription(),
                cmd.dateFinInscription()
        );

        AnneeAcademique annee =AnneeAcademique.creer(
                cmd.codeAnnee(),
                datesAnnee
        );

        repository.save(annee);
        log.info("[creerAnnee] mois générés={}", annee.getMoisAcademiques().size());

        annee.pullDomainEvents()
                .forEach(
                        outboxPort::save
                );
    }
}

