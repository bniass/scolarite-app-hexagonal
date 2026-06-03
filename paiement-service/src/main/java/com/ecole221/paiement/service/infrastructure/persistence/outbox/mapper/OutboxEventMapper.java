package com.ecole221.paiement.service.infrastructure.persistence.outbox.mapper;

import com.ecole221.common.avro.AvroSerializerUtil;
import com.ecole221.common.avro.PaiementConfirmeAvroModel;
import com.ecole221.paiement.service.domain.event.DossierInitialiseEvent;
import com.ecole221.paiement.service.infrastructure.persistence.outbox.entity.OutboxEventJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventMapper {

    public OutboxEventJpaEntity toJpa(DossierInitialiseEvent event) {
        try {
            PaiementConfirmeAvroModel avroModel = PaiementConfirmeAvroModel.newBuilder()
                    .setInscriptionId(event.getInscriptionId())
                    .setStatut(event.getStatut())
                    .setMessage(event.getMessage() != null ? event.getMessage() : "")
                    .build();

            byte[] payload = AvroSerializerUtil.toBytes(avroModel);

            return new OutboxEventJpaEntity(
                    event.aggregateType(),
                    event.aggregateId(),
                    event.getClass().getSimpleName(),
                    payload,
                    event.occurredAt()
            );
        } catch (Exception e) {
            throw new RuntimeException("Erreur sérialisation outbox paiement", e);
        }
    }

    public OutboxEventJpaEntity toJpaEchec(String inscriptionId, String message) {
        try {
            PaiementConfirmeAvroModel avroModel = PaiementConfirmeAvroModel.newBuilder()
                    .setInscriptionId(inscriptionId)
                    .setStatut("ECHEC")
                    .setMessage(message != null ? message : "")
                    .build();

            byte[] payload = AvroSerializerUtil.toBytes(avroModel);

            return new OutboxEventJpaEntity(
                    "DossierPaiement",
                    inscriptionId,
                    "DossierEchecEvent",
                    payload,
                    java.time.LocalDateTime.now()
            );
        } catch (Exception e) {
            throw new RuntimeException("Erreur sérialisation outbox échec paiement", e);
        }
    }
}
