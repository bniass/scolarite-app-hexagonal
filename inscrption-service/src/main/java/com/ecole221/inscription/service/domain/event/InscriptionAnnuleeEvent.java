package com.ecole221.inscription.service.domain.event;

import java.time.LocalDateTime;

public class InscriptionAnnuleeEvent extends InscriptionEvent {

    public InscriptionAnnuleeEvent(String inscriptionId, LocalDateTime occurredAt) {
        super(inscriptionId, occurredAt);
    }
}
