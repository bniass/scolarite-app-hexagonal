package com.ecole221.paiement.service.application.port.out;

import com.ecole221.paiement.service.domain.model.DossierPaiement;

import java.util.Optional;
import java.util.UUID;

public interface DossierPaiementRepository {
    DossierPaiement sauvegarder(DossierPaiement dossier);
    Optional<DossierPaiement> trouverParInscriptionId(UUID inscriptionId);
}
