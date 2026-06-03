package com.ecole221.school.service.application.usecase;

import com.ecole221.school.service.application.command.CreerFiliereCommand;
import com.ecole221.school.service.application.port.in.CreerFiliereUseCase;
import com.ecole221.school.service.application.port.out.outbox.OutboxPort;
import com.ecole221.school.service.application.port.out.repository.FiliereRepository;
import com.ecole221.school.service.domain.exception.SchoolException;
import com.ecole221.school.service.domain.model.Filiere;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CreerFiliereService implements CreerFiliereUseCase {

    private final FiliereRepository filiereRepository;
    private final OutboxPort outboxPort;

    public CreerFiliereService(FiliereRepository filiereRepository, OutboxPort outboxPort) {
        this.filiereRepository = filiereRepository;
        this.outboxPort = outboxPort;
    }

    @Override
    public UUID executer(CreerFiliereCommand command) {
        if (filiereRepository.findByCode(command.code()).isPresent()) {
            throw new SchoolException("Une filière avec le code '" + command.code() + "' existe déjà");
        }
        Filiere filiere = Filiere.creer(command.code(), command.nom());
        filiereRepository.save(filiere);
        filiere.pullDomainEvents().forEach(outboxPort::save);
        return filiere.getId().getValue();
    }
}
