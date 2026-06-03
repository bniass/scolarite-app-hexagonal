package com.ecole221.school.service.application.port.out.repository;

import com.ecole221.school.service.domain.model.Tarif;
import com.ecole221.school.service.domain.model.TarifId;

import java.util.Optional;

public interface TarifRepository {
    void save(Tarif tarif);
    Optional<Tarif> findById(TarifId id);
}
