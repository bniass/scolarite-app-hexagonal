package com.ecole221.paiement.service.application.port.in;

import java.util.UUID;

public interface SupprimerDossierUseCase {
    void supprimerParInscriptionId(UUID inscriptionId);
}
