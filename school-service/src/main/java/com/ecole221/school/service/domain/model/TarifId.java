package com.ecole221.school.service.domain.model;

import com.ecole221.common.valueobject.BaseId;

import java.util.UUID;

public class TarifId extends BaseId<UUID> {
    public TarifId(UUID value) {
        super(value);
    }

    public static TarifId generate() {
        return new TarifId(UUID.randomUUID());
    }

    public static TarifId of(UUID id) {
        return new TarifId(id);
    }
}
