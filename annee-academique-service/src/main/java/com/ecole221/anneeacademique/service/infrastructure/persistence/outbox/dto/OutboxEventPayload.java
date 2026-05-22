package com.ecole221.anneeacademique.service.infrastructure.persistence.outbox.dto;

import java.time.LocalDateTime;

public record OutboxEventPayload(
        String aggregateType,
        String aggregateId,
        String eventType,
        LocalDateTime occurredAt
) {}
