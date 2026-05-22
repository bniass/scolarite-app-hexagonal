package com.ecole221.anneeacademique.service.infrastructure.event.outbox;

import com.ecole221.common.avro.AvroSerializerUtil;
import com.ecole221.common.avro.CreateAnneeAcademiqueAvroModel;
import com.ecole221.kafka.service.producer.KafkaMessageHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import com.ecole221.anneeacademique.service.infrastructure.persistence.outbox.entity.OutboxEventJpaEntity;
import com.ecole221.anneeacademique.service.infrastructure.persistence.outbox.entity.OutboxStatus;
import com.ecole221.anneeacademique.service.infrastructure.persistence.outbox.repository.OutboxEventJpaRepository;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OutboxPublisher {
    @Value("${kafka-topics.anneeacademique-topic-request-name}")
    private String topic;

    private final KafkaMessageHelper<String, CreateAnneeAcademiqueAvroModel> kafkaMessageHelper;
    private final OutboxEventJpaRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxEventJpaRepository repository,
                           ObjectMapper objectMapper,
                           KafkaMessageHelper<String, CreateAnneeAcademiqueAvroModel> kafkaMessageHelper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.kafkaMessageHelper = kafkaMessageHelper;
    }

    @Scheduled(fixedDelay = 5000) // toutes les 5 secondes
    public void publish() {
        List<OutboxEventJpaEntity> events =
                repository.findTop50ByStatusInOrderByOccurredAtAsc(List.of(OutboxStatus.PENDING, OutboxStatus.FAILED));

        for (OutboxEventJpaEntity event : events) {
             CreateAnneeAcademiqueAvroModel eventToSend;
            try {
                CreateAnneeAcademiqueAvroModel createAnneeAcademiqueAvroModel =
                        AvroSerializerUtil.fromBytes(event.getPayload(), CreateAnneeAcademiqueAvroModel.class);
                kafkaMessageHelper.send(
                        topic,
                        event.getAggregateId(), // clé Kafka
                        createAnneeAcademiqueAvroModel
                );

                event.setStatus(OutboxStatus.PUBLISHED);
                repository.save(event);
            } catch (Exception e) {
                e.printStackTrace();
                event.setStatus(OutboxStatus.FAILED);
                repository.save(event);
            }
        }
    }

}
