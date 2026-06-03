package com.ecole221.school.service.domain.event;

import com.ecole221.common.event.DomainEvent;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.LocalDateTime;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@type")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
                getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public abstract class SchoolEvent implements DomainEvent {

    private final String aggregateId;
    private final LocalDateTime occurredAt;

    protected SchoolEvent(String aggregateId, LocalDateTime occurredAt) {
        this.aggregateId = aggregateId;
        this.occurredAt = occurredAt;
    }

    @Override
    public String aggregateId() { return aggregateId; }

    @Override
    public LocalDateTime occurredAt() { return occurredAt; }
}
