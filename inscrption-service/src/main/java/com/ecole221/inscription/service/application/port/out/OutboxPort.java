package com.ecole221.inscription.service.application.port.out;

import com.ecole221.common.event.DomainEvent;

public interface OutboxPort {

    void sauvegarder(DomainEvent event);
}
