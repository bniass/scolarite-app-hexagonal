package com.ecole221.school.service.domain.model;

import java.time.LocalDate;

public class ClasseTarif {

    private final TarifId tarifId;
    private final LocalDate dateActivation;
    private LocalDate dateDesactivation;
    private boolean actif;

    public ClasseTarif(TarifId tarifId, LocalDate dateActivation) {
        this.tarifId = tarifId;
        this.dateActivation = dateActivation;
        this.actif = true;
    }

    private ClasseTarif(TarifId tarifId, LocalDate dateActivation, LocalDate dateDesactivation, boolean actif) {
        this.tarifId = tarifId;
        this.dateActivation = dateActivation;
        this.dateDesactivation = dateDesactivation;
        this.actif = actif;
    }

    public static ClasseTarif reconstituer(TarifId tarifId, LocalDate dateActivation,
                                           LocalDate dateDesactivation, boolean actif) {
        return new ClasseTarif(tarifId, dateActivation, dateDesactivation, actif);
    }

    public void desactiver(LocalDate dateDesactivation) {
        this.actif = false;
        this.dateDesactivation = dateDesactivation;
    }

    public TarifId getTarifId() { return tarifId; }
    public LocalDate getDateActivation() { return dateActivation; }
    public LocalDate getDateDesactivation() { return dateDesactivation; }
    public boolean isActif() { return actif; }
}
