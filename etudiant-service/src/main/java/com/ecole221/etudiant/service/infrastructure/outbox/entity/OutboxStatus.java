package com.ecole221.etudiant.service.infrastructure.outbox.entity;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED,
    DEAD_LETTER
}
