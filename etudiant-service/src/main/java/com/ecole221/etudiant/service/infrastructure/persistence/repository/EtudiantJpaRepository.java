package com.ecole221.etudiant.service.infrastructure.persistence.repository;

import com.ecole221.etudiant.service.infrastructure.persistence.entity.EtudiantJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EtudiantJpaRepository extends JpaRepository<EtudiantJpaEntity, UUID> {
    Optional<EtudiantJpaEntity> findByMatricule(String matricule);
    boolean existsByMatricule(String matricule);
}
