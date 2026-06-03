package com.ecole221.school.service.application.command;

import com.ecole221.school.service.domain.model.Cycle;
import com.ecole221.school.service.domain.model.Niveau;

import java.util.UUID;

public record CreerClasseCommand(
        String code,
        String nom,
        Cycle cycle,
        Niveau niveau,
        UUID filiereId
) {}
