package com.ecole221.etudiant.service.infrastructure.persistence.repository;

import com.ecole221.etudiant.service.infrastructure.persistence.entity.EtudiantJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface EtudiantJpaRepository extends JpaRepository<EtudiantJpaEntity, UUID> {
    Optional<EtudiantJpaEntity> findByMatricule(String matricule);
    boolean existsByMatricule(String matricule);
    boolean existsByEmail(String email);

    @Query("SELECT COUNT(e) FROM EtudiantJpaEntity e WHERE e.matricule LIKE %:suffixe")
    long countBySuffixeAnnee(@Param("suffixe") String suffixeAnnee);
}
