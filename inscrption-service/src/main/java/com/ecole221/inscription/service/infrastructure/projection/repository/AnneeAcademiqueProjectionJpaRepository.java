package com.ecole221.inscription.service.infrastructure.projection.repository;

import com.ecole221.inscription.service.infrastructure.projection.entity.AnneeAcademiqueProjectionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnneeAcademiqueProjectionJpaRepository
        extends JpaRepository<AnneeAcademiqueProjectionJpaEntity, String> {

}
