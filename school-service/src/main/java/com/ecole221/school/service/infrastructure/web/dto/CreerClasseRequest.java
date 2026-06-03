package com.ecole221.school.service.infrastructure.web.dto;

import com.ecole221.school.service.domain.model.Cycle;
import com.ecole221.school.service.domain.model.Niveau;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreerClasseRequest(
        @NotBlank(message = "Le code est obligatoire") String code,
        @NotBlank(message = "Le nom est obligatoire") String nom,
        @NotNull(message = "Le cycle est obligatoire") Cycle cycle,
        @NotNull(message = "Le niveau est obligatoire") Niveau niveau,
        @NotNull(message = "L'identifiant de la filière est obligatoire") UUID filiereId
) {}
