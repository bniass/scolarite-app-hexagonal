package com.ecole221.anneeacademique.service.application.usecase;

import com.ecole221.anneeacademique.service.application.port.in.ListerAnneesAcademiquesUseCase;
import com.ecole221.anneeacademique.service.application.port.out.repository.AnneeAcademiqueRepository;
import com.ecole221.anneeacademique.service.domain.model.AnneeAcademique;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ListerAnneesAcademiquesService implements ListerAnneesAcademiquesUseCase {

    private final AnneeAcademiqueRepository repository;

    @Override
    public List<AnneeAcademique> executer() {
        return repository.findAll();
    }
}
