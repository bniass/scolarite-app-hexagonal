package com.ecole221.inscription.service.application.port.in;

import com.ecole221.inscription.service.application.command.SynchroniserAnneeAcademiqueCommand;

public interface SynchroniserAnneeAcademiqueUseCase {
    void synchroniser(SynchroniserAnneeAcademiqueCommand command);
}
