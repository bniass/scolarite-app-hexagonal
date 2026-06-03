package com.ecole221.anneeacademique.service.domain.event;

import com.ecole221.anneeacademique.service.domain.model.MoisAcademique;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public class AnneeAcademiqueCreeeEvent extends AnneeAcademiqueEvent {

    private final List<MoisAcademique> moisAcademiques;

    @JsonCreator
    public AnneeAcademiqueCreeeEvent(
            @JsonProperty("code") String code,
            @JsonProperty("etatAnnee") String statutAnnee,
            @JsonProperty("occurredAt") LocalDateTime occurredAt,
            @JsonProperty("moisAcademiques") List<MoisAcademique> moisAcademiques
    ) {
        super(code, statutAnnee, occurredAt);
        this.moisAcademiques = moisAcademiques != null ? moisAcademiques : List.of();
    }

    public List<MoisAcademique> getMoisAcademiques() {
        return moisAcademiques;
    }
}
