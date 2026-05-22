package com.ecole221.common.event;

import java.time.LocalDateTime;

public interface DomainEvent<T> {
    String aggregateId();

    String aggregateType();

    LocalDateTime occurredAt();
}
