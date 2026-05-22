package com.ecole221.anneeacademique.service.domain.event;


import com.ecole221.anneeacademique.service.domain.state.EtatAnnee;

import java.time.LocalDateTime;

public final class InscriptionsOuvertesEvent extends AnneeAcademiqueEvent {

    public InscriptionsOuvertesEvent(String code, String statutAnnee) {
        super(code, statutAnnee, LocalDateTime.now());
    }
}
