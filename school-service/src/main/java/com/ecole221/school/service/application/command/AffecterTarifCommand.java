package com.ecole221.school.service.application.command;

import java.util.UUID;

public record AffecterTarifCommand(
        UUID classeId,
        UUID tarifId
) {}
