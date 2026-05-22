package com.ecole221.anneeacademique.service.infrastructure.persistence.outbox;


import com.ecole221.anneeacademique.service.application.port.out.outbox.OutboxPort;
import com.ecole221.anneeacademique.service.domain.event.AnneeAcademiqueClotureeEvent;
import com.ecole221.anneeacademique.service.domain.event.AnneeAcademiqueCreeeEvent;
import com.ecole221.anneeacademique.service.domain.event.AnneeAcademiqueEvent;
import com.ecole221.anneeacademique.service.infrastructure.event.mapper.AnneeMapper;
import com.ecole221.common.avro.AvroSerializerUtil;
import com.ecole221.anneeacademique.service.infrastructure.persistence.outbox.entity.OutboxEventJpaEntity;
import com.ecole221.anneeacademique.service.infrastructure.persistence.outbox.entity.OutboxStatus;
import com.ecole221.anneeacademique.service.infrastructure.persistence.outbox.repository.OutboxEventJpaRepository;
import com.ecole221.common.avro.CreateAnneeAcademiqueAvroModel;
import com.ecole221.common.event.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventJpaAdapter implements OutboxPort {

    private final OutboxEventJpaRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxEventJpaAdapter(OutboxEventJpaRepository repository,
                                 ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(DomainEvent event) {

        try {
            CreateAnneeAcademiqueAvroModel avro =
                    AnneeMapper.toAvro((AnneeAcademiqueEvent) event);

            byte[] payload = AvroSerializerUtil.toBytes(avro); // ✔ FIX

            OutboxEventJpaEntity entity = new OutboxEventJpaEntity();

            entity.setAggregateType(event.aggregateType());
            entity.setAggregateId(event.aggregateId());
            entity.setEventType(event.getClass().getSimpleName());
            entity.setPayload(payload);
            entity.setStatus(OutboxStatus.PENDING);

            repository.save(entity);

        } catch (Exception e) {
            throw new RuntimeException("Erreur serialization Outbox", e);
        }
    }
}
