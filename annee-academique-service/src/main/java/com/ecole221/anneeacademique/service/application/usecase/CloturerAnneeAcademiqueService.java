package com.ecole221.anneeacademique.service.application.usecase;

import com.ecole221.anneeacademique.service.application.command.CloturerAnneeAcademiqueCommand;
import com.ecole221.anneeacademique.service.application.port.in.CloturerAnneeScolaireUseCase;
import com.ecole221.anneeacademique.service.application.port.out.outbox.OutboxPort;
import com.ecole221.anneeacademique.service.application.port.out.repository.AnneeAcademiqueRepository;
import com.ecole221.anneeacademique.service.domain.model.AnneeAcademique;
import com.ecole221.anneeacademique.service.domain.exception.AnneeAcademiqueNotFoundException;
import com.ecole221.anneeacademique.service.domain.valuobject.CodeAnnee;
import com.ecole221.common.event.publisher.DomainEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class CloturerAnneeAcademiqueService
        implements CloturerAnneeScolaireUseCase {

    private final AnneeAcademiqueRepository repository;
    private final DomainEventPublisher eventPublisher;
    private final OutboxPort outboxPort;

    public CloturerAnneeAcademiqueService(
            AnneeAcademiqueRepository repository,
            DomainEventPublisher eventPublisher, OutboxPort outboxPort
    ) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.outboxPort = outboxPort;
    }

    @Override
    public void executer(CloturerAnneeAcademiqueCommand cmd) {

        AnneeAcademique annee = repository.findByCode(new CodeAnnee(cmd.codeAnnee()).getCodeAnnee())
                .orElseThrow(() ->
                        new AnneeAcademiqueNotFoundException(
                                "Année académique introuvable"
                        )
                );

        annee.cloturer();

        repository.save(annee);
        annee.pullDomainEvents()
                .forEach(outboxPort::save);
    }
}

