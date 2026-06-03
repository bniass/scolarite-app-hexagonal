package com.ecole221.school.service.infrastructure.web.dto;

import com.ecole221.school.service.domain.model.Cycle;
import com.ecole221.school.service.domain.model.Niveau;

import java.math.BigDecimal;
import java.util.UUID;

public record TarifActifResponse(
        UUID classeId,
        String code,
        String nom,
        Cycle cycle,
        Niveau niveau,
        UUID tarifId,
        BigDecimal fraisInscription,
        BigDecimal mensualite,
        BigDecimal autresFrais
) {}
