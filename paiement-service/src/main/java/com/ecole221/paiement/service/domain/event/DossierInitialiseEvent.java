package com.ecole221.paiement.service.domain.event;

import java.time.LocalDateTime;

public class DossierInitialiseEvent extends DossierPaiementEvent {
    private final String statut;
    private final String message;

    public DossierInitialiseEvent(String inscriptionId, String statut, String message, LocalDateTime occurredAt) {
        super(inscriptionId, occurredAt);
        this.statut = statut;
        this.message = message;
    }

    public String getStatut() { return statut; }
    public String getMessage() { return message; }
}
