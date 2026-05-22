package com.ecole221.etudiant.service.infrastructure.outbox.repository;

import com.ecole221.etudiant.service.infrastructure.outbox.entity.OutboxEventJpaEntity;
import com.ecole221.etudiant.service.infrastructure.outbox.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, Long> {
    List<OutboxEventJpaEntity> findTop50ByStatusInOrderByOccurredAtAsc(List<OutboxStatus> statuses);
}
