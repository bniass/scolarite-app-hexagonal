package com.ecole221.etudiant.service.domain.valueobject;

import java.util.Objects;

public class Matricule {

    private final String valeur;

    public Matricule(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException("Le matricule ne peut pas être vide");
        }
        this.valeur = valeur.toUpperCase().trim();
    }

    public String getValeur() {
        return valeur;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Matricule m)) return false;
        return valeur.equals(m.valeur);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valeur);
    }

    @Override
    public String toString() {
        return valeur;
    }
}
