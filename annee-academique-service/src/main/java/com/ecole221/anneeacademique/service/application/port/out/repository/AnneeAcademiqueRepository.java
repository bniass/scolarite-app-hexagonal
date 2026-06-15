package com.ecole221.anneeacademique.service.application.port.out.repository;

import com.ecole221.anneeacademique.service.domain.model.AnneeAcademique;
import com.ecole221.anneeacademique.service.domain.model.Statut;

import java.util.List;
import java.util.Optional;


public interface AnneeAcademiqueRepository {
    Optional<AnneeAcademique> findByCode(String code);
    boolean existsByStatutNot(Statut statut);
    void save(AnneeAcademique anneeAcademique);
    List<AnneeAcademique> findAll();
}
