package com.ecole221.anneeacademique.service.infrastructure.persistence.outbox;

import com.ecole221.anneeacademique.service.infrastructure.persistence.outbox.entity.OutboxEventJpaEntity;
import com.ecole221.anneeacademique.service.infrastructure.persistence.outbox.mapper.OutboxEventMapper;
import com.ecole221.anneeacademique.service.infrastructure.persistence.outbox.repository.OutboxEventJpaRepository;
import com.ecole221.common.event.DomainEvent;
import com.ecole221.common.event.publisher.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("mysql")
@RequiredArgsConstructor
public class OutboxDomainEventPublisher implements DomainEventPublisher {

    private final OutboxEventJpaRepository repository;
    private final OutboxEventMapper mapper;

    @Override
    public void publish(DomainEvent event) {
        repository.save(mapper.toJpa(event));
    }
}
