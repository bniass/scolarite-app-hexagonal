package com.ecole221.anneeacademique.service.infrastructure.persistence.repository;

import com.ecole221.anneeacademique.service.domain.model.Statut;
import com.ecole221.anneeacademique.service.infrastructure.persistence.entity.AnneeAcademiqueJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnneeAcademiqueJpaRepository
        extends JpaRepository<AnneeAcademiqueJpaEntity, Long> {
    @Query("""
            select a from AnneeAcademiqueJpaEntity a
            left join fetch a.moisAcademiques
            where a.code = :code
            """)
    Optional<AnneeAcademiqueJpaEntity> findByCode(String code);
    boolean existsByStatutNot(Statut statut);

}

