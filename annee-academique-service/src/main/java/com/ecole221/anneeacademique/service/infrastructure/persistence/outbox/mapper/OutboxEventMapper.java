package com.ecole221.anneeacademique.service.infrastructure.persistence.outbox.mapper;

import com.ecole221.anneeacademique.service.domain.event.AnneeAcademiqueCreeeEvent;
import com.ecole221.anneeacademique.service.domain.model.MoisAcademique;
import com.ecole221.common.avro.AvroSerializerUtil;
import com.ecole221.anneeacademique.service.infrastructure.persistence.outbox.dto.OutboxEventPayload;
import com.ecole221.anneeacademique.service.infrastructure.persistence.outbox.entity.OutboxEventJpaEntity;
import com.ecole221.anneeacademique.service.infrastructure.persistence.outbox.exception.OutboxSerializationException;
import com.ecole221.common.avro.CreateAnneeAcademiqueAvroModel;
import com.ecole221.common.event.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventMapper {

    private final ObjectMapper objectMapper;

    public OutboxEventJpaEntity toJpa(DomainEvent event) {
        try {
            AnneeAcademiqueCreeeEvent anneeEvent = (AnneeAcademiqueCreeeEvent) event;

            log.info("[OutboxEventMapper] etat={} mois={}", anneeEvent.getEtatAnnee(), anneeEvent.getMoisAcademiques());
            String moisJson = serializerMois(anneeEvent.getMoisAcademiques());

            CreateAnneeAcademiqueAvroModel avroModel = CreateAnneeAcademiqueAvroModel.newBuilder()
                    .setCodeAnnee(anneeEvent.getCode())
                    .setEtatAnnee(anneeEvent.getEtatAnnee())
                    .setMoisAcademiques(moisJson)
                    .build();

            byte[] payload = AvroSerializerUtil.toBytes(avroModel);
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
                    "Erreur de sérialisation de l'événement " + event.getClass().getSimpleName(), e);
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

    private static String serializerMois(List<MoisAcademique> mois) {
        if (mois == null || mois.isEmpty()) return "[]";
        return mois.stream()
                .map(m -> "{\"mois\":" + m.mois() + ",\"annee\":" + m.annee() + "}")
                .collect(Collectors.joining(",", "[", "]"));
    }
}
