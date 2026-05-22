package com.ecole221.anneeacademique.service.application.port.in;


import com.ecole221.anneeacademique.service.application.command.FermerInscriptionCommand;

public interface FermerInscriptionUseCase {
    void executer(FermerInscriptionCommand command);
}
