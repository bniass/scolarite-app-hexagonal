package com.ecole221.anneeacademique.service.infrastructure.persistence.outbox.mapper;

import com.ecole221.anneeacademique.service.domain.event.AnneeAcademiqueCreeeEvent;
import com.ecole221.common.avro.AvroSerializerUtil;
import com.ecole221.anneeacademique.service.infrastructure.persistence.outbox.dto.OutboxEventPayload;
import com.ecole221.anneeacademique.service.infrastructure.persistence.outbox.entity.OutboxEventJpaEntity;
import com.ecole221.anneeacademique.service.infrastructure.persistence.outbox.exception.OutboxSerializationException;
import com.ecole221.common.avro.CreateAnneeAcademiqueAvroModel;
import com.ecole221.common.event.DomainEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxEventMapper {

    private final ObjectMapper objectMapper;

    public OutboxEventJpaEntity toJpa(DomainEvent event) {

        try {
            AnneeAcademiqueCreeeEvent anneeAcademiqueCreeeEvent =
                    (AnneeAcademiqueCreeeEvent)event;
            CreateAnneeAcademiqueAvroModel createAnneeAcademiqueAvroModel =
                    CreateAnneeAcademiqueAvroModel.newBuilder()
                            .setCodeAnnee(anneeAcademiqueCreeeEvent.getCode())
                            .setEtatAnnee(anneeAcademiqueCreeeEvent.getEtatAnnee())
                            .build();

            byte[] payload = AvroSerializerUtil.toBytes(createAnneeAcademiqueAvroModel);
            objectMapper.registerModule(new JavaTimeModule());

            return new OutboxEventJpaEntity(
                    event.aggregateType(),
                    event.aggregateId(),
                    event.getClass().getSimpleName(),
                    payload,
                    event.occurredAt()
            );

        } catch (Exception e) {
            e.printStackTrace();
            throw new OutboxSerializationException(
                    "Erreur de sérialisation de l'événement " + event.getClass().getSimpleName(),
                    e
            );
        }
    }


    public static OutboxEventPayload from(DomainEvent event) {
        return new OutboxEventPayload(
                event.aggregateType(),
                event.aggregateId(),
                event.getClass().getSimpleName(),
                event.occurredAt()
        );
    }
}
