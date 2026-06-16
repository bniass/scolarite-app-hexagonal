package com.ecole221.paiement.service.infrastructure.kafka.consumer;

import com.ecole221.common.avro.InscriptionTransfereAvroModel;
import com.ecole221.kafka.service.consumer.KafkaConsumer;
import com.ecole221.paiement.service.application.command.InitialiserDossierCommand;
import com.ecole221.paiement.service.application.port.in.TransfererDossierUseCase;
import com.ecole221.paiement.service.domain.model.MoisAcademique;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class InscriptionTransfereKafkaConsumer implements KafkaConsumer<InscriptionTransfereAvroModel> {

    private final TransfererDossierUseCase transfererDossierUseCase;
    private final ObjectMapper objectMapper;

    @Override
    @KafkaListener(topics = "${kafka-topics.inscription-transfere-topic}",
            groupId = "${kafka-consumer.group-id}")
    public void receive(List<InscriptionTransfereAvroModel> messages, List<String> keys,
            List<Integer> partitions, List<Long> offsets) {
        for (int i = 0; i < messages.size(); i++) {
            InscriptionTransfereAvroModel msg = messages.get(i);
            try {
                List<MoisAcademique> mois = parseMois(msg.getMoisAcademiques());
                InitialiserDossierCommand command = new InitialiserDossierCommand(
                        UUID.fromString(msg.getInscriptionId()),
                        UUID.fromString(msg.getEtudiantId()),
                        UUID.fromString(msg.getNouvelleClasseId()),
                        msg.getCodeAnnee(),
                        new BigDecimal(msg.getFraisInscription()),
                        new BigDecimal(msg.getMensualite()),
                        new BigDecimal(msg.getAutresFrais()),
                        mois
                );
                transfererDossierUseCase.executer(command);
                log.info("Dossier transféré pour inscription {}", msg.getInscriptionId());
            } catch (Exception e) {
                log.error("Erreur transfert dossier pour inscription {} : {}",
                        msg.getInscriptionId(), e.getMessage(), e);
            }
        }
    }

    private List<MoisAcademique> parseMois(String json) {
        try {
            List<Map<String, Integer>> raw = objectMapper.readValue(json, new TypeReference<>() {});
            return raw.stream()
                    .map(m -> new MoisAcademique(m.get("mois"), m.get("annee")))
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Impossible de parser les mois académiques (json=" + json + ")", e);
        }
    }
}
