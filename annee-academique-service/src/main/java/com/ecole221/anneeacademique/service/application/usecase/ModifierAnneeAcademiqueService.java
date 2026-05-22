package com.ecole221.anneeacademique.service.application.usecase;

import com.ecole221.anneeacademique.service.application.command.ModifierAnneeAcademiqueCommand;
import com.ecole221.anneeacademique.service.application.port.in.ModifierAnneeAcademiqueUseCase;
import com.ecole221.anneeacademique.service.application.port.out.repository.AnneeAcademiqueRepository;
import com.ecole221.anneeacademique.service.domain.model.AnneeAcademique;
import com.ecole221.anneeacademique.service.domain.model.DatesAnnee;
import com.ecole221.anneeacademique.service.domain.exception.AnneeAcademiqueNotFoundException;
import com.ecole221.anneeacademique.service.domain.valuobject.CodeAnnee;
import com.ecole221.common.event.publisher.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ModifierAnneeAcademiqueService
        implements ModifierAnneeAcademiqueUseCase {

    private final AnneeAcademiqueRepository repository;
    private final DomainEventPublisher eventPublisher;


    public ModifierAnneeAcademiqueService(
            AnneeAcademiqueRepository repository, DomainEventPublisher eventPublisher
    ) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    @Override
    public void executer(ModifierAnneeAcademiqueCommand cmd) {


        Optional<AnneeAcademique> annee = repository
                .findByCode(new CodeAnnee(cmd.code()).getCodeAnnee());
        if (!annee.isPresent()) {
            throw new AnneeAcademiqueNotFoundException(
                    "Cette année académique n'existe pas!"
            );
        }

        AnneeAcademique toUpdated = annee.get();
        DatesAnnee datesAnnee = new DatesAnnee(cmd.dateDebut(),
                cmd.dateFin(),
                cmd.dateDebutInscriptions(),
                cmd.dateFinInscriptions());
        toUpdated.modifier(datesAnnee);

        repository.save(toUpdated);

        //toUpdated.pullDomainEvents()
                //.forEach(eventPublisher::publish);
    }
}

