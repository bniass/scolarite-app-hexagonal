package com.ecole221.school.service.infrastructure.persistence.outbox.repository;

import com.ecole221.school.service.infrastructure.persistence.outbox.entity.OutboxEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, Long> {
}
