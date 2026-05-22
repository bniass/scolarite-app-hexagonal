package com.ecole221.anneeacademique.service.application.port.in;


import com.ecole221.anneeacademique.service.application.command.CloturerAnneeAcademiqueCommand;

public interface CloturerAnneeScolaireUseCase {
    void executer(CloturerAnneeAcademiqueCommand command);
}
