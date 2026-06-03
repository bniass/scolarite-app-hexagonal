package com.ecole221.school.service.domain.event;

import java.time.LocalDateTime;

public class TarifCreeEvent extends SchoolEvent {

    private static final String AGGREGATE_TYPE = "Tarif";

    public TarifCreeEvent(String aggregateId, LocalDateTime occurredAt) {
        super(aggregateId, occurredAt);
    }

    @Override
    public String aggregateType() { return AGGREGATE_TYPE; }
}
