package com.ecole221.etudiant.service.application.port.in;

import com.ecole221.etudiant.service.application.command.ModifierEtudiantCommand;

public interface ModifierEtudiantUseCase {
    void executer(ModifierEtudiantCommand command);
}
