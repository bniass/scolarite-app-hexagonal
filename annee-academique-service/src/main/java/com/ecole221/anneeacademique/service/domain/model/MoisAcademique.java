package com.ecole221.anneeacademique.service.domain.model;

import java.util.Objects;

public final class MoisAcademique {

    private final int mois;   // 1–12
    private final int annee;

    public MoisAcademique(int mois, int annee) {
        if (mois < 1 || mois > 12) {
            throw new IllegalArgumentException("Mois invalide");
        }
        this.mois = mois;
        this.annee = annee;
    }

    public int mois() {
        return mois;
    }

    public int annee() {
        return annee;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MoisAcademique that)) return false;
        return mois == that.mois && annee == that.annee;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mois, annee);
    }
}
