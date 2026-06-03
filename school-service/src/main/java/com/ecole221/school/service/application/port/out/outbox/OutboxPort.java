package com.ecole221.school.service.application.port.out.outbox;

import com.ecole221.common.event.DomainEvent;

public interface OutboxPort {
    void save(DomainEvent event);
}
