package com.ecole221.school.service.infrastructure.persistence.repository;

import com.ecole221.school.service.infrastructure.persistence.entity.FiliereJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FiliereJpaRepository extends JpaRepository<FiliereJpaEntity, UUID> {
    Optional<FiliereJpaEntity> findByCode(String code);
}
