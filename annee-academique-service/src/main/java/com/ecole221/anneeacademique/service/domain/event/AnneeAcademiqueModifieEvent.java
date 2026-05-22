package com.ecole221.anneeacademique.service.domain.event;

import java.time.LocalDateTime;

public class AnneeAcademiqueModifieEvent extends AnneeAcademiqueEvent {
    public AnneeAcademiqueModifieEvent(String code, String statutAnnee) {
        super(code, statutAnnee, LocalDateTime.now());
    }
}
