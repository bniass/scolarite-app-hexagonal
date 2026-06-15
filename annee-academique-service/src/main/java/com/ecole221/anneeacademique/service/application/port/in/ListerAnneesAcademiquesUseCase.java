package com.ecole221.anneeacademique.service.application.port.in;

import com.ecole221.anneeacademique.service.domain.model.AnneeAcademique;

import java.util.List;

public interface ListerAnneesAcademiquesUseCase {
    List<AnneeAcademique> executer();
}
