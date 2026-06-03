package com.ecole221.school.service.infrastructure.persistence.outbox.entity;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
