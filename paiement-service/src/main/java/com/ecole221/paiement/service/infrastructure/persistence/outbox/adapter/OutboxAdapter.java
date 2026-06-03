package com.ecole221.paiement.service.infrastructure.persistence.outbox.adapter;

import com.ecole221.paiement.service.application.port.out.PaiementOutboxPort;
import com.ecole221.paiement.service.domain.event.DossierInitialiseEvent;
import com.ecole221.paiement.service.infrastructure.persistence.outbox.mapper.OutboxEventMapper;
import com.ecole221.paiement.service.infrastructure.persistence.outbox.repository.OutboxEventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxAdapter implements PaiementOutboxPort {

    private final OutboxEventJpaRepository repository;
    private final OutboxEventMapper mapper;

    @Override
    public void sauvegarder(DossierInitialiseEvent event) {
        repository.save(mapper.toJpa(event));
    }

    @Override
    public void sauvegarderEchec(String inscriptionId, String message) {
        repository.save(mapper.toJpaEchec(inscriptionId, message));
    }
}
