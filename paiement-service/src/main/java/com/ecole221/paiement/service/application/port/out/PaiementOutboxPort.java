package com.ecole221.paiement.service.application.port.out;

import com.ecole221.paiement.service.domain.event.DossierInitialiseEvent;

public interface PaiementOutboxPort {
    void sauvegarder(DossierInitialiseEvent event);
    /** Publie un événement d'échec vers inscription-service pour déclencher la compensation. */
    void sauvegarderEchec(String inscriptionId, String message);
}
