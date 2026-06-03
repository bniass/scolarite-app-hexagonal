package com.ecole221.paiement.service.application.port.in;

import com.ecole221.paiement.service.application.command.DistribuerVersementCommand;
import com.ecole221.paiement.service.domain.model.DossierPaiement;

public interface DistribuerVersementUseCase {
    DossierPaiement executer(DistribuerVersementCommand command);
}
