package com.ecole221.school.service.domain.event;

import java.time.LocalDateTime;

public class FiliereCreeeEvent extends SchoolEvent {

    private final String code;
    private static final String AGGREGATE_TYPE = "Filiere";

    public FiliereCreeeEvent(String aggregateId, String code, LocalDateTime occurredAt) {
        super(aggregateId, occurredAt);
        this.code = code;
    }

    @Override
    public String aggregateType() { return AGGREGATE_TYPE; }

    public String getCode() { return code; }
}
