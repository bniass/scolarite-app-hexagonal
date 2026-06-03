package com.ecole221.paiement.service.application.port.in;

import com.ecole221.paiement.service.application.command.InitialiserDossierCommand;
import com.ecole221.paiement.service.domain.model.DossierPaiement;

public interface InitialiserDossierUseCase {
    DossierPaiement executer(InitialiserDossierCommand command);
}
