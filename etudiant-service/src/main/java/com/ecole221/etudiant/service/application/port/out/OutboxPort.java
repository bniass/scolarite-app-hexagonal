package com.ecole221.etudiant.service.application.port.out;

import com.ecole221.common.event.DomainEvent;

public interface OutboxPort {
    void sauvegarder(DomainEvent event);
}
