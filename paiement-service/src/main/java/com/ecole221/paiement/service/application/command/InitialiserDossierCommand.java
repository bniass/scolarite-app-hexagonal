package com.ecole221.paiement.service.application.command;

import com.ecole221.paiement.service.domain.model.MoisAcademique;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record InitialiserDossierCommand(
        UUID inscriptionId,
        UUID etudiantId,
        UUID classeId,
        String codeAnnee,
        BigDecimal fraisInscription,
        BigDecimal mensualite,
        BigDecimal autresFrais,
        List<MoisAcademique> moisAcademiques
) {}
