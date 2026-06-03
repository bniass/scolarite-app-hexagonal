package com.ecole221.inscription.service.application.command;

import com.ecole221.inscription.service.domain.valueobject.CodeAnnee;
import com.ecole221.inscription.service.domain.valueobject.EtatAnnee;

public record SynchroniserAnneeAcademiqueCommand(
        CodeAnnee codeAnnee,
        EtatAnnee etatAnnee,
        String moisAcademiquesJson
) {
}
