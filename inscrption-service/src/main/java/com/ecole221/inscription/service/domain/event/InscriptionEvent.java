package com.ecole221.inscription.service.domain.event;

import com.ecole221.common.event.DomainEvent;

import java.time.LocalDateTime;

public abstract class InscriptionEvent implements DomainEvent<String> {
    private final String inscriptionId;
    private final LocalDateTime occurredAt;

    protected InscriptionEvent(String inscriptionId, LocalDateTime occurredAt) {
        this.inscriptionId = inscriptionId;
        this.occurredAt = occurredAt;
    }

    @Override public String aggregateId() { return inscriptionId; }
    @Override public String aggregateType() { return "Inscription"; }
    @Override public LocalDateTime occurredAt() { return occurredAt; }
    public String getInscriptionId() { return inscriptionId; }
}
