package com.ecole221.inscription.service.infrastructure.persistence.outbox.publisher;

import com.ecole221.common.avro.AvroSerializerUtil;
import com.ecole221.common.avro.InscriptionAnnuleeAvroModel;
import com.ecole221.common.avro.InscriptionCreeeAvroModel;
import com.ecole221.kafka.service.producer.KafkaMessageHelper;
import com.ecole221.inscription.service.infrastructure.persistence.outbox.entity.OutboxEventJpaEntity;
import com.ecole221.inscription.service.infrastructure.persistence.outbox.entity.OutboxStatus;
import com.ecole221.inscription.service.infrastructure.persistence.outbox.repository.OutboxEventJpaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OutboxPublisher {

    @Value("${kafka-topics.inscription-creee-topic}")
    private String inscriptionCreeeTopique;

    @Value("${kafka-topics.inscription-annulee-topic}")
    private String inscriptionAnnuleeTopique;

    private final KafkaMessageHelper<String, InscriptionCreeeAvroModel> inscriptionCreeeHelper;
    private final KafkaMessageHelper<String, InscriptionAnnuleeAvroModel> inscriptionAnnuleeHelper;
    private final OutboxEventJpaRepository repository;

    public OutboxPublisher(OutboxEventJpaRepository repository,
            KafkaMessageHelper<String, InscriptionCreeeAvroModel> inscriptionCreeeHelper,
            KafkaMessageHelper<String, InscriptionAnnuleeAvroModel> inscriptionAnnuleeHelper) {
        this.repository = repository;
        this.inscriptionCreeeHelper = inscriptionCreeeHelper;
        this.inscriptionAnnuleeHelper = inscriptionAnnuleeHelper;
    }

    @Scheduled(fixedDelay = 5000)
    public void publish() {
        List<OutboxEventJpaEntity> events =
                repository.findTop50ByStatusInOrderByOccurredAtAsc(List.of(OutboxStatus.PENDING, OutboxStatus.FAILED));

        for (OutboxEventJpaEntity event : events) {
            try {
                dispatch(event);
                event.markPublished();
                repository.save(event);
            } catch (Exception e) {
                e.printStackTrace();
                event.markFailed(e.getMessage());
                repository.save(event);
            }
        }
    }

    private void dispatch(OutboxEventJpaEntity event) throws Exception {
        switch (event.getEventType()) {
            case "InscriptionCreeeEvent" -> {
                InscriptionCreeeAvroModel model =
                        AvroSerializerUtil.fromBytes(event.getPayload(), InscriptionCreeeAvroModel.class);
                inscriptionCreeeHelper.send(inscriptionCreeeTopique, event.getAggregateId(), model);
            }
            case "InscriptionAnnuleeEvent" -> {
                InscriptionAnnuleeAvroModel model =
                        AvroSerializerUtil.fromBytes(event.getPayload(), InscriptionAnnuleeAvroModel.class);
                inscriptionAnnuleeHelper.send(inscriptionAnnuleeTopique, event.getAggregateId(), model);
            }
            default -> throw new IllegalArgumentException(
                    "Type d'événement outbox non supporté : " + event.getEventType());
        }
    }
}
