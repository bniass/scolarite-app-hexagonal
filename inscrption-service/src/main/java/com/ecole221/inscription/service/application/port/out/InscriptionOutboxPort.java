package com.ecole221.inscription.service.application.port.out;

import com.ecole221.inscription.service.domain.event.InscriptionCreeeEvent;

public interface InscriptionOutboxPort {
    void sauvegarder(InscriptionCreeeEvent event);
}
