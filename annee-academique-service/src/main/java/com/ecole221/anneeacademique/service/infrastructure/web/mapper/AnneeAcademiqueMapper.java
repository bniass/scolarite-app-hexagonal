package com.ecole221.anneeacademique.service.infrastructure.web.mapper;


import com.ecole221.anneeacademique.service.application.command.CreerAnneeAcademiqueCommand;
import com.ecole221.anneeacademique.service.application.command.ModifierAnneeAcademiqueCommand;
import com.ecole221.anneeacademique.service.infrastructure.web.dto.CreerAnneeAcademiqueRequest;

public class AnneeAcademiqueMapper {
    public static CreerAnneeAcademiqueCommand toCommand(
            CreerAnneeAcademiqueRequest req
    ) {
        return new CreerAnneeAcademiqueCommand(
                Integer.parseInt(req.code()),
                req.dateDebut(),
                req.dateFin(),
                req.dateDebutInscriptions(),
                req.dateFinInscriptions()
        );
    }

    public static ModifierAnneeAcademiqueCommand toModiferCommand(
            ModifierAnneeAcademiqueCommand req
    ) {
        return new ModifierAnneeAcademiqueCommand(
                req.code(),
                req.dateDebut(),
                req.dateFin(),
                req.dateDebutInscriptions(),
                req.dateFinInscriptions()
        );
    }
}
