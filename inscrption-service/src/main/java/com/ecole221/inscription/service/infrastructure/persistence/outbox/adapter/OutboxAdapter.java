package com.ecole221.inscription.service.infrastructure.persistence.outbox.adapter;

import com.ecole221.common.event.DomainEvent;
import com.ecole221.inscription.service.application.port.out.InscriptionOutboxPort;
import com.ecole221.inscription.service.infrastructure.persistence.outbox.mapper.OutboxEventMapper;
import com.ecole221.inscription.service.infrastructure.persistence.outbox.repository.OutboxEventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxAdapter implements InscriptionOutboxPort {

    private final OutboxEventJpaRepository repository;
    private final OutboxEventMapper mapper;

    @Override
    public void sauvegarder(DomainEvent<?> event) {
        repository.save(mapper.toJpa(event));
    }
}
