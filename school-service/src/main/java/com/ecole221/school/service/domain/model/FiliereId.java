package com.ecole221.school.service.domain.model;

import com.ecole221.common.valueobject.BaseId;

import java.util.UUID;

public class FiliereId extends BaseId<UUID> {
    public FiliereId(UUID value) {
        super(value);
    }

    public static FiliereId generate() {
        return new FiliereId(UUID.randomUUID());
    }

    public static FiliereId of(UUID id) {
        return new FiliereId(id);
    }
}
