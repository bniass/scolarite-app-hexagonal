package com.ecole221.paiement.service.infrastructure.kafka.consumer;

import com.ecole221.common.avro.InscriptionAnnuleeAvroModel;
import com.ecole221.kafka.service.consumer.KafkaConsumer;
import com.ecole221.paiement.service.application.port.in.SupprimerDossierUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class InscriptionAnnuleeKafkaConsumer implements KafkaConsumer<InscriptionAnnuleeAvroModel> {

    private final SupprimerDossierUseCase supprimerDossierUseCase;

    @Override
    @KafkaListener(topics = "${kafka-topics.inscription-annulee-topic}",
            groupId = "${kafka-consumer.group-id}")
    public void receive(List<InscriptionAnnuleeAvroModel> messages, List<String> keys,
            List<Integer> partitions, List<Long> offsets) {
        for (int i = 0; i < messages.size(); i++) {
            String inscriptionId = messages.get(i).getInscriptionId();
            try {
                supprimerDossierUseCase.supprimerParInscriptionId(UUID.fromString(inscriptionId));
                log.info("Dossier supprimé suite à annulation inscription {}", inscriptionId);
            } catch (Exception e) {
                log.error("Erreur suppression dossier pour inscription annulée {} : {}",
                        inscriptionId, e.getMessage(), e);
            }
        }
    }
}
