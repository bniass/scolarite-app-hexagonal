package com.ecole221.anneeacademique.service.application.port.in;


import com.ecole221.anneeacademique.service.application.command.OuvrirInscriptionCommand;

public interface OuvrirInscriptionUseCase {
    void executer(OuvrirInscriptionCommand command);
}
