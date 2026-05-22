package com.ecole221.etudiant.service.application.port.in;

import com.ecole221.etudiant.service.application.command.CreerEtudiantCommand;
import com.ecole221.etudiant.service.domain.model.Etudiant;

public interface CreerEtudiantUseCase {
    Etudiant executer(CreerEtudiantCommand command);
}
