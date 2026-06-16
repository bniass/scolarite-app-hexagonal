package com.ecole221.inscription.service.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record AnnulerInscriptionRequest(
        @NotBlank(message = "Le motif d'annulation est obligatoire")
        String motif
) {}
