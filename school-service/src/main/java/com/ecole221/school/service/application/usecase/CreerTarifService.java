package com.ecole221.school.service.application.usecase;

import com.ecole221.school.service.application.command.CreerTarifCommand;
import com.ecole221.school.service.application.port.in.CreerTarifUseCase;
import com.ecole221.school.service.application.port.out.outbox.OutboxPort;
import com.ecole221.school.service.application.port.out.repository.TarifRepository;
import com.ecole221.school.service.domain.model.Tarif;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CreerTarifService implements CreerTarifUseCase {

    private final TarifRepository tarifRepository;
    private final OutboxPort outboxPort;

    public CreerTarifService(TarifRepository tarifRepository, OutboxPort outboxPort) {
        this.tarifRepository = tarifRepository;
        this.outboxPort = outboxPort;
    }

    @Override
    public UUID executer(CreerTarifCommand command) {
        Tarif tarif = Tarif.creer(command.fraisInscription(), command.mensualite(), command.autresFrais());
        tarifRepository.save(tarif);
        tarif.pullDomainEvents().forEach(outboxPort::save);
        return tarif.getId().getValue();
    }
}
