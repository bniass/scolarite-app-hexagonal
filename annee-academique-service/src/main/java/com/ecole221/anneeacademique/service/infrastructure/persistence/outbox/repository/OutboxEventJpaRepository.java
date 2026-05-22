package com.ecole221.anneeacademique.service.infrastructure.persistence.outbox.repository;

import com.ecole221.anneeacademique.service.infrastructure.persistence.outbox.entity.OutboxEventJpaEntity;
import com.ecole221.anneeacademique.service.infrastructure.persistence.outbox.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventJpaRepository
        extends JpaRepository<OutboxEventJpaEntity, Long> {

    List<OutboxEventJpaEntity>
    findTop50ByStatusInOrderByOccurredAtAsc(
            List<OutboxStatus> statuses
    );
}
