package com.ecole221.paiement.service.application.port.in;

import com.ecole221.paiement.service.application.command.InitialiserDossierCommand;

public interface TransfererDossierUseCase {
    void executer(InitialiserDossierCommand command);
}
