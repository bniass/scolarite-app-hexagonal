package com.ecole221.school.service.infrastructure.persistence.repository;

import com.ecole221.school.service.infrastructure.persistence.entity.ClasseJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClasseJpaRepository extends JpaRepository<ClasseJpaEntity, UUID> {
    Optional<ClasseJpaEntity> findByCode(String code);
}
