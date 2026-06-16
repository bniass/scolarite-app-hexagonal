package com.ecole221.inscription.service.application.port.out;

import com.ecole221.common.event.DomainEvent;

public interface InscriptionOutboxPort {
    void sauvegarder(DomainEvent<?> event);
}
