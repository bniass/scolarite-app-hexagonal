package com.ecole221.anneeacademique.service.application.port.in;


import com.ecole221.anneeacademique.service.application.command.PublierAnneeAcademiqueCommand;

public interface PublierAnneeAcademiqueUseCase {
    void executer(PublierAnneeAcademiqueCommand command);
}
