package com.ecole221.anneeacademique.service.infrastructure.persistence.outbox.dto;

public record EventPayload(
        String code,
        String etatAnnee
) {
}
