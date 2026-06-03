package com.ecole221.paiement.service.infrastructure.kafka.consumer;

import com.ecole221.common.avro.InscriptionCreeeAvroModel;
import com.ecole221.kafka.service.consumer.KafkaConsumer;
import com.ecole221.paiement.service.application.command.InitialiserDossierCommand;
import com.ecole221.paiement.service.application.port.in.InitialiserDossierUseCase;
import com.ecole221.paiement.service.application.port.out.DossierPaiementRepository;
import com.ecole221.paiement.service.application.port.out.PaiementOutboxPort;
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
public class InscriptionCreeeKafkaConsumer implements KafkaConsumer<InscriptionCreeeAvroModel> {

    private final InitialiserDossierUseCase initialiserDossierUseCase;
    private final DossierPaiementRepository dossierRepository;
    private final PaiementOutboxPort outboxPort;
    private final ObjectMapper objectMapper;

    @Override
    @KafkaListener(topics = "${kafka-topics.inscription-creee-topic}",
            groupId = "${kafka-consumer.group-id}")
    public void receive(List<InscriptionCreeeAvroModel> messages, List<String> keys,
            List<Integer> partitions, List<Long> offsets) {
        for (int i = 0; i < messages.size(); i++) {
            String inscriptionId = messages.get(i).getInscriptionId();
            try {
                process(messages.get(i));
            } catch (Exception e) {
                log.error("Erreur traitement inscription-creee : clé={} inscriptionId={}",
                        keys.get(i), inscriptionId, e);
                compenserEchec(inscriptionId, e.getMessage());
            }
        }
    }

    private void process(InscriptionCreeeAvroModel model) {
        UUID inscriptionId = UUID.fromString(model.getInscriptionId());

        // Idempotence : si le dossier existe déjà (message re-délivré), on ignore
        if (dossierRepository.trouverParInscriptionId(inscriptionId).isPresent()) {
            log.warn("Dossier déjà existant pour inscription {}, message ignoré", inscriptionId);
            return;
        }

        List<MoisAcademique> mois = parseMois(model.getMoisAcademiques());

        InitialiserDossierCommand command = new InitialiserDossierCommand(
                inscriptionId,
                UUID.fromString(model.getEtudiantId()),
                UUID.fromString(model.getClasseId()),
                model.getCodeAnnee(),
                new BigDecimal(model.getFraisInscription()),
                new BigDecimal(model.getMensualite()),
                new BigDecimal(model.getAutresFrais()),
                mois,
                new BigDecimal(model.getMontantVerse()),
                model.getTypePaiement(),
                model.getOperateur(),
                model.getReferencePaiement(),
                model.getNomBanque(),
                model.getNumeroTransaction()
        );

        initialiserDossierUseCase.executer(command);
        log.info("Dossier initialisé pour inscription {}", inscriptionId);
    }

    private void compenserEchec(String inscriptionId, String message) {
        try {
            outboxPort.sauvegarderEchec(inscriptionId,
                    "Echec initialisation dossier : " + (message != null ? message : "erreur inconnue"));
            log.warn("Compensation publiée pour inscription {}", inscriptionId);
        } catch (Exception ex) {
            log.error("Impossible de publier la compensation pour inscription {} : {}",
                    inscriptionId, ex.getMessage());
        }
    }

    private List<MoisAcademique> parseMois(String json) {
        try {
            List<Map<String, Integer>> raw = objectMapper.readValue(json, new TypeReference<>() {});
            List<MoisAcademique> result = raw.stream()
                    .map(m -> new MoisAcademique(m.get("mois"), m.get("annee")))
                    .toList();
            if (result.isEmpty()) {
                throw new RuntimeException("La liste des mois académiques est vide dans le message Kafka (json=" + json + ")");
            }
            return result;
        } catch (RuntimeException e) {
            throw e; // propage pour déclencher la compensation
        } catch (Exception e) {
            throw new RuntimeException("Impossible de parser les mois académiques (json=" + json + ")", e);
        }
    }
}
