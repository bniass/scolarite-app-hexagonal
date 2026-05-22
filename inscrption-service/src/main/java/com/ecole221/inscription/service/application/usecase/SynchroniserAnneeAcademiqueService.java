package com.ecole221.inscription.service.application.usecase;

import com.ecole221.inscription.service.application.command.SynchroniserAnneeAcademiqueCommand;
import com.ecole221.inscription.service.application.port.in.SynchroniserAnneeAcademiqueUseCase;
import com.ecole221.inscription.service.application.port.out.repository.AnneeAcademiqueProjectionRepository;
import com.ecole221.inscription.service.domain.model.projection.AnneeAcademiqueProjection;
import com.ecole221.inscription.service.domain.valueobject.CodeAnnee;
import com.ecole221.inscription.service.domain.valueobject.EtatAnnee;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SynchroniserAnneeAcademiqueService implements SynchroniserAnneeAcademiqueUseCase {

    private final AnneeAcademiqueProjectionRepository repository;

    public SynchroniserAnneeAcademiqueService(AnneeAcademiqueProjectionRepository repository) {
        this.repository = repository;
    }

    @Override
    public void synchroniser(SynchroniserAnneeAcademiqueCommand command) {

        CodeAnnee codeAnnee = new CodeAnnee(command.codeAnnee().getValue());
        EtatAnnee etatAnnee = EtatAnnee.valueOf(command.etatAnnee().name());

        AnneeAcademiqueProjection projection = repository.findByCodeAnnee(codeAnnee.getValue())
                .map(existing -> {
                    existing.changerEtat(etatAnnee);
                    return existing;
                })
                .orElseGet(() -> new AnneeAcademiqueProjection(codeAnnee, etatAnnee));

        repository.save(projection);
    }
}
