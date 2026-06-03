package com.ecole221.school.service.domain.model;

import com.ecole221.common.valueobject.BaseId;

import java.util.UUID;

public class ClasseId extends BaseId<UUID> {
    public ClasseId(UUID value) {
        super(value);
    }

    public static ClasseId generate() {
        return new ClasseId(UUID.randomUUID());
    }

    public static ClasseId of(UUID id) {
        return new ClasseId(id);
    }
}
