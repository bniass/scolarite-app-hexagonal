package com.ecole221.school.service.application.usecase;

import com.ecole221.school.service.application.command.AffecterTarifCommand;
import com.ecole221.school.service.application.port.in.AffecterTarifUseCase;
import com.ecole221.school.service.application.port.out.outbox.OutboxPort;
import com.ecole221.school.service.application.port.out.repository.ClasseRepository;
import com.ecole221.school.service.application.port.out.repository.TarifRepository;
import com.ecole221.school.service.domain.exception.SchoolNotFoundException;
import com.ecole221.school.service.domain.model.Classe;
import com.ecole221.school.service.domain.model.ClasseId;
import com.ecole221.school.service.domain.model.TarifId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AffecterTarifService implements AffecterTarifUseCase {

    private final ClasseRepository classeRepository;
    private final TarifRepository tarifRepository;
    private final OutboxPort outboxPort;

    public AffecterTarifService(ClasseRepository classeRepository,
                                 TarifRepository tarifRepository,
                                 OutboxPort outboxPort) {
        this.classeRepository = classeRepository;
        this.tarifRepository = tarifRepository;
        this.outboxPort = outboxPort;
    }

    @Override
    public void executer(AffecterTarifCommand command) {
        ClasseId classeId = ClasseId.of(command.classeId());
        TarifId tarifId = TarifId.of(command.tarifId());

        Classe classe = classeRepository.findById(classeId)
                .orElseThrow(() -> new SchoolNotFoundException("Classe introuvable : " + command.classeId()));

        tarifRepository.findById(tarifId)
                .orElseThrow(() -> new SchoolNotFoundException("Tarif introuvable : " + command.tarifId()));

        classe.affecterTarif(tarifId);
        classeRepository.save(classe);
        classe.pullDomainEvents().forEach(outboxPort::save);
    }
}
