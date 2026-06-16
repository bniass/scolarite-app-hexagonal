package com.ecole221.inscription.service.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public record TarifActifResult(
        UUID tarifId,
        BigDecimal fraisInscription,
        BigDecimal mensualite,
        BigDecimal autresFrais,
        String niveau
) {
    public TarifActifResult(UUID tarifId, BigDecimal fraisInscription,
            BigDecimal mensualite, BigDecimal autresFrais) {
        this(tarifId, fraisInscription, mensualite, autresFrais, null);
    }
}
