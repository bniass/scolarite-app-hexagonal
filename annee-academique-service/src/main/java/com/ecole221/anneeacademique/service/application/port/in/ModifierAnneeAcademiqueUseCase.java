package com.ecole221.anneeacademique.service.application.port.in;


import com.ecole221.anneeacademique.service.application.command.ModifierAnneeAcademiqueCommand;

public interface ModifierAnneeAcademiqueUseCase {
    void executer(ModifierAnneeAcademiqueCommand command);

}
