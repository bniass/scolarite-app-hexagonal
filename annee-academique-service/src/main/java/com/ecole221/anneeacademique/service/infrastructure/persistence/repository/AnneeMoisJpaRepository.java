package com.ecole221.anneeacademique.service.infrastructure.persistence.repository;

import com.ecole221.anneeacademique.service.infrastructure.persistence.entity.AnneeMoisJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnneeMoisJpaRepository extends JpaRepository<AnneeMoisJpaEntity, Long> {
    Optional<AnneeMoisJpaEntity> findByAnneeAcademiqueCodeAndMoisAndAnnee(
            String anneeAcademiqueCode,
            int mois,
            int annee
    );

}
