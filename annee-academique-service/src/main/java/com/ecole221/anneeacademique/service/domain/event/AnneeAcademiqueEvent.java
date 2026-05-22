package com.ecole221.anneeacademique.service.domain.event;

import com.ecole221.common.event.DomainEvent;

import java.time.LocalDateTime;

public abstract class AnneeAcademiqueEvent implements DomainEvent {

    private final String code;
    private final String statutAnnee;
    private final LocalDateTime occurredAt;
    private static final String AGGREGATE_TYPE = "AnneeAcademique";

    protected AnneeAcademiqueEvent(
            String code,
            String statutAnnee,
            LocalDateTime occurredAt
    ) {
        this.code = code;
        this.statutAnnee = statutAnnee;
        this.occurredAt = occurredAt;
    }

    public String getCode() { return code; }

    public String getEtatAnnee() { return statutAnnee; }

    @Override
    public String aggregateType() { return AGGREGATE_TYPE; }

    @Override
    public String aggregateId() { return code; }

    @Override
    public LocalDateTime occurredAt() { return occurredAt; }
}