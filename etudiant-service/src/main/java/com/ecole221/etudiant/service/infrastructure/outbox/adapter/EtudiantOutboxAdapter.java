package com.ecole221.etudiant.service.infrastructure.outbox.adapter;

import com.ecole221.common.avro.AvroSerializerUtil;
import com.ecole221.common.event.DomainEvent;
import com.ecole221.etudiant.service.application.port.out.OutboxPort;
import com.ecole221.etudiant.service.domain.event.EtudiantCreeEvent;
import com.ecole221.etudiant.service.domain.event.EtudiantModifieEvent;
import com.ecole221.etudiant.service.infrastructure.outbox.entity.OutboxEventJpaEntity;
import com.ecole221.etudiant.service.infrastructure.outbox.entity.OutboxStatus;
import com.ecole221.etudiant.service.infrastructure.outbox.mapper.EtudiantMapper;
import com.ecole221.etudiant.service.infrastructure.outbox.repository.OutboxEventJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class EtudiantOutboxAdapter implements OutboxPort {

    private final OutboxEventJpaRepository repository;

    public EtudiantOutboxAdapter(OutboxEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void sauvegarder(DomainEvent event) {
        try {
            byte[] payload = switch (event) {
                case EtudiantCreeEvent e     -> AvroSerializerUtil.toBytes(EtudiantMapper.toAvro(e));
                case EtudiantModifieEvent e  -> AvroSerializerUtil.toBytes(EtudiantMapper.toAvro(e));
                default -> throw new IllegalArgumentException("Type d'événement non supporté : "
                        + event.getClass().getSimpleName());
            };

            OutboxEventJpaEntity entity = new OutboxEventJpaEntity();
            entity.setAggregateType(event.aggregateType());
            entity.setAggregateId(event.aggregateId());
            entity.setEventType(event.getClass().getSimpleName());
            entity.setPayload(payload);
            entity.setOccurredAt(event.occurredAt());
            entity.setStatus(OutboxStatus.PENDING);
            repository.save(entity);
        } catch (Exception e) {
            throw new RuntimeException("Erreur sérialisation événement outbox : " + e.getMessage() + " — cause: " + (e.getCause() != null ? e.getCause().getMessage() : "null"), e);
        }
    }
}
