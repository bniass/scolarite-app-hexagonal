package com.ecole221.school.service.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreerFiliereRequest(
        @NotBlank(message = "Le code est obligatoire") String code,
        @NotBlank(message = "Le nom est obligatoire") String nom
) {}
