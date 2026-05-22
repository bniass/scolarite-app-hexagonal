package com.ecole221.anneeacademique.service.application.port.out.repository;

import com.ecole221.anneeacademique.service.domain.model.AnneeAcademique;

import java.util.Optional;


public interface AnneeAcademiqueRepository {
    Optional<AnneeAcademique> findByCode(String code);

    void save(AnneeAcademique anneeAcademique);
}
