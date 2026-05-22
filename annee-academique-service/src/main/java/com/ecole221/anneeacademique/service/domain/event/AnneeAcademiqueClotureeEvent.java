package com.ecole221.anneeacademique.service.domain.event;


import com.ecole221.anneeacademique.service.domain.state.EtatAnnee;

import java.time.LocalDateTime;

public class AnneeAcademiqueClotureeEvent extends AnneeAcademiqueEvent {

    public AnneeAcademiqueClotureeEvent(String code, String statutAnnee) {
        super(code, statutAnnee, LocalDateTime.now());
    }

}

