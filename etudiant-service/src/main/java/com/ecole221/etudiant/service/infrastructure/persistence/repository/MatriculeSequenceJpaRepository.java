package com.ecole221.etudiant.service.infrastructure.persistence.repository;

import com.ecole221.etudiant.service.infrastructure.persistence.entity.MatriculeSequenceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatriculeSequenceJpaRepository extends JpaRepository<MatriculeSequenceJpaEntity, Long> {
}
