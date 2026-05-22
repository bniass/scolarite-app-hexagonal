package com.ecole221.etudiant.service.domain.valueobject;

import com.ecole221.common.valueobject.BaseId;

import java.util.UUID;

public class EtudiantId extends BaseId<UUID> {
    public EtudiantId(UUID value) {
        super(value);
    }
}
