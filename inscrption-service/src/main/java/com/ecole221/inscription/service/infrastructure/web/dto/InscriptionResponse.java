package com.ecole221.inscription.service.infrastructure.web.dto;

import com.ecole221.inscription.service.domain.model.Inscription;
import com.ecole221.inscription.service.domain.valueobject.StatutInscription;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record InscriptionResponse(
        UUID id,
        UUID etudiantId,
        UUID classeId,
        String codeAnnee,
        BigDecimal fraisInscription,
        BigDecimal mensualite,
        BigDecimal autresFrais,
        StatutInscription statut,
        LocalDateTime creeLe,
        List<MoisAcademiqueDto> moisAcademiques
) {
    // Ordre de paiement : juin=1, oct=2, nov=3, déc=4, jan=5, fév=6, mar=7, avr=8, mai=9
    private static final Map<Integer, Integer> ORDRE_PAIEMENT = Map.of(
            6, 1, 10, 2, 11, 3, 12, 4, 1, 5, 2, 6, 3, 7, 4, 8, 5, 9
    );

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public record MoisAcademiqueDto(int mois, int annee, int ordrePaiement) {}

    public static InscriptionResponse from(Inscription i) {
        return new InscriptionResponse(
                i.getId(), i.getEtudiantId(), i.getClasseId(),
                i.getCodeAnnee(), i.getFraisInscription(), i.getMensualite(),
                i.getAutresFrais(), i.getStatut(), i.getCreeLe(),
                parseMoisOrdonnés(i.getMoisAcademiquesJson())
        );
    }

    private static List<MoisAcademiqueDto> parseMoisOrdonnés(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            List<Map<String, Integer>> raw = MAPPER.readValue(json, new TypeReference<>() {});
            return raw.stream()
                    .map(m -> {
                        int mois = m.get("mois");
                        int annee = m.get("annee");
                        int ordre = ORDRE_PAIEMENT.getOrDefault(mois, 99);
                        return new MoisAcademiqueDto(mois, annee, ordre);
                    })
                    .sorted(Comparator.comparingInt(MoisAcademiqueDto::ordrePaiement))
                    .toList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
