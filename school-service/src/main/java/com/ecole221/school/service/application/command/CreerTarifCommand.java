package com.ecole221.school.service.application.command;

import java.math.BigDecimal;

public record CreerTarifCommand(
        BigDecimal fraisInscription,
        BigDecimal mensualite,
        BigDecimal autresFrais
) {}
