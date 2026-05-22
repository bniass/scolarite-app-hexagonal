package com.ecole221.anneeacademique.service.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class AnneeAcademiqueCreeeEvent extends AnneeAcademiqueEvent {

    @JsonCreator
    public AnneeAcademiqueCreeeEvent(
            @JsonProperty("code") String code,
            @JsonProperty("etatAnnee") String statutAnnee,
            @JsonProperty("occurredAt") LocalDateTime occurredAt
    ) {
        super(code, statutAnnee, occurredAt);
    }
}

