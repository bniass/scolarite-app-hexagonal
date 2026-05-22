package com.ecole221.anneeacademique.service.domain.model;

import java.time.LocalDate;

public final class DatesAnnee {
    private final LocalDate dateDebut;
    private final LocalDate dateFin;
    private final LocalDate dateOuvertureInscription;
    private final LocalDate dateFinInscription;


    public DatesAnnee(LocalDate dateDebut, LocalDate dateFin, LocalDate dateOuvertureInscription, LocalDate dateFinInscription) {
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.dateOuvertureInscription = dateOuvertureInscription;
        this.dateFinInscription = dateFinInscription;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public LocalDate getDateOuvertureInscription() {
        return dateOuvertureInscription;
    }

    public LocalDate getDateFinInscription() {
        return dateFinInscription;
    }
}
