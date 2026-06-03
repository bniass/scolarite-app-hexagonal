package com.ecole221.school.service.application.usecase;

import com.ecole221.school.service.application.command.CreerClasseCommand;
import com.ecole221.school.service.application.port.in.CreerClasseUseCase;
import com.ecole221.school.service.application.port.out.outbox.OutboxPort;
import com.ecole221.school.service.application.port.out.repository.ClasseRepository;
import com.ecole221.school.service.application.port.out.repository.FiliereRepository;
import com.ecole221.school.service.domain.exception.SchoolException;
import com.ecole221.school.service.domain.exception.SchoolNotFoundException;
import com.ecole221.school.service.domain.model.Classe;
import com.ecole221.school.service.domain.model.FiliereId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CreerClasseService implements CreerClasseUseCase {

    private final ClasseRepository classeRepository;
    private final FiliereRepository filiereRepository;
    private final OutboxPort outboxPort;

    public CreerClasseService(ClasseRepository classeRepository,
                               FiliereRepository filiereRepository,
                               OutboxPort outboxPort) {
        this.classeRepository = classeRepository;
        this.filiereRepository = filiereRepository;
        this.outboxPort = outboxPort;
    }

    @Override
    public UUID executer(CreerClasseCommand command) {
        if (classeRepository.findByCode(command.code()).isPresent()) {
            throw new SchoolException("Une classe avec le code '" + command.code() + "' existe déjà");
        }
        FiliereId filiereId = FiliereId.of(command.filiereId());
        filiereRepository.findById(filiereId)
                .orElseThrow(() -> new SchoolNotFoundException("Filière introuvable : " + command.filiereId()));

        Classe classe = Classe.creer(command.code(), command.nom(), command.cycle(), command.niveau(), filiereId);
        classeRepository.save(classe);
        classe.pullDomainEvents().forEach(outboxPort::save);
        return classe.getId().getValue();
    }
}
