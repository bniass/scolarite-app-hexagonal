package com.ecole221.school.service.domain.event;

import java.time.LocalDateTime;

public class TarifAffecteEvent extends SchoolEvent {

    private final String tarifId;
    private static final String AGGREGATE_TYPE = "Classe";

    public TarifAffecteEvent(String classeId, String tarifId, LocalDateTime occurredAt) {
        super(classeId, occurredAt);
        this.tarifId = tarifId;
    }

    @Override
    public String aggregateType() { return AGGREGATE_TYPE; }

    public String getTarifId() { return tarifId; }
}
