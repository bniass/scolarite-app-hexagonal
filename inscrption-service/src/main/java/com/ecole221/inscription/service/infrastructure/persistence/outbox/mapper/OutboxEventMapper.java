package com.ecole221.inscription.service.infrastructure.persistence.outbox.mapper;

import com.ecole221.common.avro.AvroSerializerUtil;
import com.ecole221.common.avro.InscriptionAnnuleeAvroModel;
import com.ecole221.common.avro.InscriptionCreeeAvroModel;
import com.ecole221.common.event.DomainEvent;
import com.ecole221.inscription.service.domain.event.InscriptionAnnuleeEvent;
import com.ecole221.inscription.service.domain.event.InscriptionCreeeEvent;
import com.ecole221.inscription.service.infrastructure.persistence.outbox.entity.OutboxEventJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventMapper {

    public OutboxEventJpaEntity toJpa(DomainEvent<?> event) {
        try {
            byte[] payload;
            if (event instanceof InscriptionCreeeEvent e) {
                payload = AvroSerializerUtil.toBytes(InscriptionCreeeAvroModel.newBuilder()
                        .setInscriptionId(e.getInscriptionId())
                        .setEtudiantId(e.getEtudiantId().toString())
                        .setClasseId(e.getClasseId().toString())
                        .setCodeAnnee(e.getCodeAnnee())
                        .setFraisInscription(e.getFraisInscription().toPlainString())
                        .setMensualite(e.getMensualite().toPlainString())
                        .setAutresFrais(e.getAutresFrais().toPlainString())
                        .setMoisAcademiques(e.getMoisAcademiquesJson() != null
                                ? e.getMoisAcademiquesJson() : "[]")
                        .build());
            } else if (event instanceof InscriptionAnnuleeEvent e) {
                payload = AvroSerializerUtil.toBytes(InscriptionAnnuleeAvroModel.newBuilder()
                        .setInscriptionId(e.getInscriptionId())
                        .build());
            } else {
                throw new IllegalArgumentException("Type d'événement non supporté : " + event.getClass().getSimpleName());
            }
            return new OutboxEventJpaEntity(
                    event.aggregateType(),
                    event.aggregateId().toString(),
                    event.getClass().getSimpleName(),
                    payload,
                    event.occurredAt()
            );
        } catch (Exception e) {
            throw new RuntimeException("Erreur sérialisation outbox inscription", e);
        }
    }
}
