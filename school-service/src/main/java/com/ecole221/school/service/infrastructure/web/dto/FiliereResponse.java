package com.ecole221.school.service.infrastructure.web.dto;

import com.ecole221.school.service.domain.model.Filiere;

import java.util.UUID;

public record FiliereResponse(UUID id, String code, String nom) {

    public static FiliereResponse from(Filiere filiere) {
        return new FiliereResponse(filiere.getId().getValue(), filiere.getCode(), filiere.getNom());
    }
}
