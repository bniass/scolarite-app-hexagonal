package com.ecole221.inscription.service.application.port.out.repository;

import com.ecole221.inscription.service.domain.model.projection.AnneeAcademiqueProjection;

import java.util.Optional;

public interface AnneeAcademiqueProjectionRepository {

    Optional<AnneeAcademiqueProjection> findByCodeAnnee(String codeAnnee);

    void save(AnneeAcademiqueProjection projection);
}
