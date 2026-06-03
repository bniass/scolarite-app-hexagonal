package com.ecole221.school.service.infrastructure.persistence.outbox;

import com.ecole221.common.event.DomainEvent;
import com.ecole221.school.service.application.port.out.outbox.OutboxPort;
import com.ecole221.school.service.infrastructure.persistence.outbox.entity.OutboxEventJpaEntity;
import com.ecole221.school.service.infrastructure.persistence.outbox.entity.OutboxStatus;
import com.ecole221.school.service.infrastructure.persistence.outbox.repository.OutboxEventJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventJpaAdapter implements OutboxPort {

    private final OutboxEventJpaRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxEventJpaAdapter(OutboxEventJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void save(DomainEvent event) {
        try {
            byte[] payload = objectMapper.writeValueAsBytes(event);
            OutboxEventJpaEntity entity = new OutboxEventJpaEntity(
                    event.aggregateType(),
                    event.aggregateId(),
                    event.getClass().getSimpleName(),
                    payload,
                    event.occurredAt()
            );
            entity.setStatus(OutboxStatus.PENDING);
            repository.save(entity);
        } catch (Exception e) {
            throw new RuntimeException("Erreur sérialisation outbox : " + event.getClass().getSimpleName(), e);
        }
    }
}
