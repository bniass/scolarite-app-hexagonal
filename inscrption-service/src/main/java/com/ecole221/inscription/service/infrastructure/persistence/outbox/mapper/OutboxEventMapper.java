package com.ecole221.inscription.service.infrastructure.persistence.outbox.mapper;

import com.ecole221.common.avro.AvroSerializerUtil;
import com.ecole221.common.avro.InscriptionCreeeAvroModel;
import com.ecole221.inscription.service.domain.event.InscriptionCreeeEvent;
import com.ecole221.inscription.service.infrastructure.persistence.outbox.entity.OutboxEventJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventMapper {

    public OutboxEventJpaEntity toJpa(InscriptionCreeeEvent event) {
        try {
            InscriptionCreeeAvroModel avroModel = InscriptionCreeeAvroModel.newBuilder()
                    .setInscriptionId(event.getInscriptionId())
                    .setEtudiantId(event.getEtudiantId().toString())
                    .setClasseId(event.getClasseId().toString())
                    .setCodeAnnee(event.getCodeAnnee())
                    .setFraisInscription(event.getFraisInscription().toPlainString())
                    .setMensualite(event.getMensualite().toPlainString())
                    .setAutresFrais(event.getAutresFrais().toPlainString())
                    .setMoisAcademiques(event.getMoisAcademiquesJson() != null
                            ? event.getMoisAcademiquesJson() : "[]")
                    .setMontantVerse(event.getMontantVerse().toPlainString())
                    .setTypePaiement(event.getTypePaiement())
                    .setOperateur(event.getOperateur())
                    .setReferencePaiement(event.getReferencePaiement())
                    .setNomBanque(event.getNomBanque())
                    .setNumeroTransaction(event.getNumeroTransaction())
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
            throw new RuntimeException("Erreur sérialisation outbox inscription", e);
        }
    }
}
