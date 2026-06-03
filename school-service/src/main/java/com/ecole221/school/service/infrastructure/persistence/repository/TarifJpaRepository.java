package com.ecole221.school.service.infrastructure.persistence.repository;

import com.ecole221.school.service.infrastructure.persistence.entity.TarifJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TarifJpaRepository extends JpaRepository<TarifJpaEntity, UUID> {
}
