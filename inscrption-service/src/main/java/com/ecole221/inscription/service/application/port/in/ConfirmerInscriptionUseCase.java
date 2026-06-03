package com.ecole221.inscription.service.application.port.in;

import java.util.UUID;

public interface ConfirmerInscriptionUseCase {
    void executer(UUID inscriptionId);
}
