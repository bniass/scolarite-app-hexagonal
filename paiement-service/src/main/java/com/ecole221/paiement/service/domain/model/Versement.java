package com.ecole221.paiement.service.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class Versement {
    private UUID id;
    private BigDecimal montant;
    private LocalDate datePaiement;
    private MoyenPaiement moyen;

    private Versement() {}

    public static Versement creer(BigDecimal montant, LocalDate datePaiement, MoyenPaiement moyen) {
        Versement v = new Versement();
        v.id = UUID.randomUUID();
        v.montant = montant;
        v.datePaiement = datePaiement;
        v.moyen = moyen.copier();
        return v;
    }

    public static Versement reconstituer(UUID id, BigDecimal montant, LocalDate datePaiement, MoyenPaiement moyen) {
        Versement v = new Versement();
        v.id = id;
        v.montant = montant;
        v.datePaiement = datePaiement;
        v.moyen = moyen;
        return v;
    }

    public UUID getId() { return id; }
    public BigDecimal getMontant() { return montant; }
    public LocalDate getDatePaiement() { return datePaiement; }
    public MoyenPaiement getMoyen() { return moyen; }
}
