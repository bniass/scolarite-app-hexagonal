package com.ecole221.paiement.service.domain.event;

import com.ecole221.common.event.DomainEvent;

import java.time.LocalDateTime;

public abstract class DossierPaiementEvent implements DomainEvent<String> {
    private final String inscriptionId;
    private final LocalDateTime occurredAt;

    protected DossierPaiementEvent(String inscriptionId, LocalDateTime occurredAt) {
        this.inscriptionId = inscriptionId;
        this.occurredAt = occurredAt;
    }

    @Override public String aggregateId() { return inscriptionId; }
    @Override public String aggregateType() { return "DossierPaiement"; }
    @Override public LocalDateTime occurredAt() { return occurredAt; }
    public String getInscriptionId() { return inscriptionId; }
}
