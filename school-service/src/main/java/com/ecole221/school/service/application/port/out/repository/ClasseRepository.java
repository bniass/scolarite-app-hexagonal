package com.ecole221.school.service.application.port.out.repository;

import com.ecole221.school.service.domain.model.Classe;
import com.ecole221.school.service.domain.model.ClasseId;

import java.util.Optional;

public interface ClasseRepository {
    void save(Classe classe);
    Optional<Classe> findById(ClasseId id);
    Optional<Classe> findByCode(String code);
}
