package com.ecole221.anneeacademique.service.application.usecase;

import com.ecole221.anneeacademique.service.application.command.OuvrirInscriptionCommand;
import com.ecole221.anneeacademique.service.application.port.in.OuvrirInscriptionUseCase;
import com.ecole221.anneeacademique.service.application.port.out.outbox.OutboxPort;
import com.ecole221.anneeacademique.service.application.port.out.repository.AnneeAcademiqueRepository;
import com.ecole221.anneeacademique.service.domain.model.AnneeAcademique;
import com.ecole221.anneeacademique.service.domain.exception.AnneeAcademiqueNotFoundException;
import com.ecole221.anneeacademique.service.domain.valuobject.CodeAnnee;
import com.ecole221.common.event.publisher.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OuvrirInscriptionService
        implements OuvrirInscriptionUseCase {

    private final AnneeAcademiqueRepository repository;
    private final DomainEventPublisher eventPublisher;
    private final OutboxPort outboxPort;

    public OuvrirInscriptionService(
            AnneeAcademiqueRepository repository, DomainEventPublisher eventPublisher, OutboxPort outboxPort
    ) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.outboxPort = outboxPort;
    }

    @Override
    @Transactional
    public void executer(OuvrirInscriptionCommand cmd) {

        AnneeAcademique annee = repository.findByCode(new CodeAnnee(cmd.codeAnnee()).getCodeAnnee())
                .orElseThrow(() ->
                        new AnneeAcademiqueNotFoundException(
                                "Année académique introuvable"
                        )
                );

        annee.ouvrirInscription();

        repository.save(annee);

        annee.pullDomainEvents()
                .forEach(outboxPort::save);
    }
}

