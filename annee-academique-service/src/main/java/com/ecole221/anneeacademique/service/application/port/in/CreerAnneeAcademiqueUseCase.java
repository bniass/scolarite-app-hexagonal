package com.ecole221.anneeacademique.service.application.port.in;


import com.ecole221.anneeacademique.service.application.command.CreerAnneeAcademiqueCommand;

public interface CreerAnneeAcademiqueUseCase {
    void executer(CreerAnneeAcademiqueCommand command);
}
