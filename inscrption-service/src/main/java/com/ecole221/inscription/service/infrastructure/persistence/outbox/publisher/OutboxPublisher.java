package com.ecole221.inscription.service.infrastructure.persistence.outbox.publisher;

import com.ecole221.common.avro.AvroSerializerUtil;
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
    private String topic;

    private final KafkaMessageHelper<String, InscriptionCreeeAvroModel> kafkaMessageHelper;
    private final OutboxEventJpaRepository repository;

    public OutboxPublisher(OutboxEventJpaRepository repository,
            KafkaMessageHelper<String, InscriptionCreeeAvroModel> kafkaMessageHelper) {
        this.repository = repository;
        this.kafkaMessageHelper = kafkaMessageHelper;
    }

    @Scheduled(fixedDelay = 5000)
    public void publish() {
        List<OutboxEventJpaEntity> events =
                repository.findTop50ByStatusInOrderByOccurredAtAsc(List.of(OutboxStatus.PENDING, OutboxStatus.FAILED));

        for (OutboxEventJpaEntity event : events) {
            try {
                InscriptionCreeeAvroModel avroModel =
                        AvroSerializerUtil.fromBytes(event.getPayload(), InscriptionCreeeAvroModel.class);
                kafkaMessageHelper.send(topic, event.getAggregateId(), avroModel);
                event.markPublished();
                repository.save(event);
            } catch (Exception e) {
                e.printStackTrace();
                event.markFailed(e.getMessage());
                repository.save(event);
            }
        }
    }
}
