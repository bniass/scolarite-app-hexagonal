package com.ecole221.anneeacademique.service.application.usecase;


import com.ecole221.anneeacademique.service.application.command.FermerInscriptionCommand;
import com.ecole221.anneeacademique.service.application.port.in.FermerInscriptionUseCase;
import com.ecole221.anneeacademique.service.application.port.out.outbox.OutboxPort;
import com.ecole221.anneeacademique.service.application.port.out.repository.AnneeAcademiqueRepository;
import com.ecole221.anneeacademique.service.domain.model.AnneeAcademique;
import com.ecole221.anneeacademique.service.domain.exception.AnneeAcademiqueNotFoundException;
import com.ecole221.anneeacademique.service.domain.valuobject.CodeAnnee;
import com.ecole221.common.event.publisher.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FermerInscriptionService
        implements FermerInscriptionUseCase {

    private final AnneeAcademiqueRepository repository;
    private final DomainEventPublisher eventPublisher;
    private final OutboxPort outboxPort;


    public FermerInscriptionService(
            AnneeAcademiqueRepository repository, DomainEventPublisher eventPublisher, OutboxPort outboxPort
    ) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.outboxPort = outboxPort;
    }

    @Transactional
    @Override
    public void executer(FermerInscriptionCommand cmd) {

        AnneeAcademique annee = repository.findByCode(new CodeAnnee(cmd.codeAnnee()).getCodeAnnee())
                .orElseThrow(() ->
                        new AnneeAcademiqueNotFoundException(
                                "Année académique introuvable"
                        )
                );

        annee.cloturerInscription();
        repository.save(annee);
        annee.pullDomainEvents()
                .forEach(outboxPort::save);
    }
}

