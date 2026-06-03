package com.ecole221.paiement.service.application.port.in;

import com.ecole221.paiement.service.domain.model.DossierPaiement;

import java.util.UUID;

public interface ConsulterDossierUseCase {
    DossierPaiement parInscriptionId(UUID inscriptionId);
}
