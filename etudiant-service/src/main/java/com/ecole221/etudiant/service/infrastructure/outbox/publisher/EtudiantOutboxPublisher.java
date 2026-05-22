package com.ecole221.etudiant.service.infrastructure.outbox.publisher;

import com.ecole221.common.avro.AvroSerializerUtil;
import com.ecole221.common.avro.EtudiantCreeAvroModel;
import com.ecole221.common.avro.EtudiantModifieAvroModel;
import com.ecole221.etudiant.service.infrastructure.outbox.entity.OutboxEventJpaEntity;
import com.ecole221.etudiant.service.infrastructure.outbox.entity.OutboxStatus;
import com.ecole221.etudiant.service.infrastructure.outbox.repository.OutboxEventJpaRepository;
import com.ecole221.kafka.service.producer.service.KafkaProducer;
import org.apache.avro.specific.SpecificRecordBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EtudiantOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(EtudiantOutboxPublisher.class);

    @Value("${kafka-topics.etudiant-cree-topic}")
    private String etudiantCreeTopic;

    @Value("${kafka-topics.etudiant-modifie-topic}")
    private String etudiantModifieTopic;

    private final OutboxEventJpaRepository repository;
    private final KafkaProducer<String, SpecificRecordBase> kafkaProducer;

    public EtudiantOutboxPublisher(OutboxEventJpaRepository repository,
                                    KafkaProducer<String, SpecificRecordBase> kafkaProducer) {
        this.repository = repository;
        this.kafkaProducer = kafkaProducer;
    }

    @Scheduled(fixedDelay = 5000)
    public void publier() {
        List<OutboxEventJpaEntity> events = repository.findTop50ByStatusInOrderByOccurredAtAsc(
                List.of(OutboxStatus.PENDING, OutboxStatus.FAILED));

        for (OutboxEventJpaEntity event : events) {
            try {
                String topic;
                SpecificRecordBase avroModel;

                switch (event.getEventType()) {
                    case "EtudiantCreeEvent" -> {
                        topic = etudiantCreeTopic;
                        avroModel = AvroSerializerUtil.fromBytes(event.getPayload(), EtudiantCreeAvroModel.class);
                    }
                    case "EtudiantModifieEvent" -> {
                        topic = etudiantModifieTopic;
                        avroModel = AvroSerializerUtil.fromBytes(event.getPayload(), EtudiantModifieAvroModel.class);
                    }
                    default -> throw new IllegalArgumentException("Type d'événement inconnu : " + event.getEventType());
                }

                kafkaProducer.send(topic, event.getAggregateId(), avroModel, (result, ex) -> {
                    if (ex != null) {
                        event.markFailed(ex.getMessage());
                        log.error("Échec publication événement id={} : {}", event.getId(), ex.getMessage());
                    } else {
                        event.markPublished();
                        log.info("Événement publié : type={}, aggregateId={}, partition={}, offset={}",
                                event.getEventType(), event.getAggregateId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                    repository.save(event);
                });
            } catch (Exception e) {
                event.markFailed(e.getMessage());
                log.error("Échec publication événement id={} : {}", event.getId(), e.getMessage());
                repository.save(event);
            }
        }
    }
}
