package com.ecole221.inscription.service.application.port.in;

import com.ecole221.inscription.service.application.command.CreerInscriptionCommand;
import com.ecole221.inscription.service.domain.model.Inscription;

public interface CreerInscriptionUseCase {
    Inscription executer(CreerInscriptionCommand command);
}
