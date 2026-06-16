package com.ecole221.inscription.service.application.port.in;

import com.ecole221.inscription.service.application.command.ChangerClasseCommand;
import com.ecole221.inscription.service.domain.model.Inscription;

public interface ChangerClasseUseCase {
    Inscription executer(ChangerClasseCommand command);
}
