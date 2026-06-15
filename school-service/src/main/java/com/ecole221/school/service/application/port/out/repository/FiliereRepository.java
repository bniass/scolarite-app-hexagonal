package com.ecole221.school.service.application.port.out.repository;

import com.ecole221.school.service.domain.model.Filiere;
import com.ecole221.school.service.domain.model.FiliereId;

import java.util.List;
import java.util.Optional;

public interface FiliereRepository {
    void save(Filiere filiere);
    Optional<Filiere> findById(FiliereId id);
    Optional<Filiere> findByCode(String code);
    List<Filiere> findAll();
}
