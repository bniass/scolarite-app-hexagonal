package com.ecole221.inscription.service.infrastructure.kafka.consumer;

import com.ecole221.common.avro.PaiementConfirmeAvroModel;
import com.ecole221.kafka.service.consumer.KafkaConsumer;
import com.ecole221.inscription.service.application.port.in.AnnulerInscriptionUseCase;
import com.ecole221.inscription.service.application.port.in.ConfirmerInscriptionUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaiementConfirmeKafkaConsumer implements KafkaConsumer<PaiementConfirmeAvroModel> {

    private final ConfirmerInscriptionUseCase confirmerUC;
    private final AnnulerInscriptionUseCase annulerUC;

    @Override
    @KafkaListener(topics = "${kafka-topics.paiement-confirme-topic}",
            groupId = "${kafka-consumer.group-id}")
    public void receive(List<PaiementConfirmeAvroModel> messages, List<String> keys,
            List<Integer> partitions, List<Long> offsets) {
        for (int i = 0; i < messages.size(); i++) {
            try {
                process(messages.get(i));
            } catch (Exception e) {
                log.error("Erreur traitement paiement-confirme : clé={}", keys.get(i), e);
            }
        }
    }

    private void process(PaiementConfirmeAvroModel model) {
        UUID inscriptionId = UUID.fromString(model.getInscriptionId());
        if ("CONFIRME".equals(model.getStatut())) {
            confirmerUC.executer(inscriptionId);
            log.info("Inscription {} confirmée", inscriptionId);
        } else {
            annulerUC.executer(inscriptionId, model.getMessage());
            log.info("Inscription {} annulée, motif: {}", inscriptionId, model.getMessage());
        }
    }
}
