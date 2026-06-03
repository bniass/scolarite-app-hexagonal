package com.ecole221.paiement.service.domain.model;

import com.ecole221.paiement.service.domain.exception.PaiementDomainException;
import com.ecole221.paiement.service.domain.valueobject.StatutLigne;
import com.ecole221.paiement.service.domain.valueobject.TypeLigne;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.format.DateTimeFormatter;

public class LignePaiement {
    private UUID id;
    private TypeLigne type;
    private MoisAcademique moisAcademique;
    private int ordreReglement;
    private BigDecimal montantDu;
    private BigDecimal montantPaye;
    private String commentaire;
    private StatutLigne statut;
    private List<Versement> versements;

    private LignePaiement() {}

    public static LignePaiement creer(TypeLigne type, MoisAcademique moisAcademique,
            int ordreReglement, BigDecimal montantDu, String commentaire) {
        LignePaiement l = new LignePaiement();
        l.id = UUID.randomUUID();
        l.type = type;
        l.moisAcademique = moisAcademique;
        l.ordreReglement = ordreReglement;
        l.montantDu = montantDu;
        l.montantPaye = BigDecimal.ZERO;
        l.commentaire = commentaire;
        l.statut = StatutLigne.APAYER;
        l.versements = new ArrayList<>();
        return l;
    }

    public static LignePaiement reconstituer(UUID id, TypeLigne type, MoisAcademique moisAcademique,
            int ordreReglement, BigDecimal montantDu, BigDecimal montantPaye,
            String commentaire, StatutLigne statut, List<Versement> versements) {
        LignePaiement l = new LignePaiement();
        l.id = id;
        l.type = type;
        l.moisAcademique = moisAcademique;
        l.ordreReglement = ordreReglement;
        l.montantDu = montantDu;
        l.montantPaye = montantPaye;
        l.commentaire = commentaire;
        l.statut = statut;
        l.versements = versements != null ? new ArrayList<>(versements) : new ArrayList<>();
        return l;
    }

    public void appliquerVersement(Versement versement) {
        if (statut == StatutLigne.PAYE) {
            throw new PaiementDomainException("La ligne " + id + " est déjà payée");
        }
        versements.add(versement);
        montantPaye = montantPaye.add(versement.getMontant());

        if (montantPaye.compareTo(montantDu) >= 0) {
            statut = StatutLigne.PAYE;
        } else {
            statut = StatutLigne.AVANCE;
        }

        // Appendre un commentaire horodaté pour conserver l'historique
        String typeMoyen = versement.getMoyen() != null
                ? versement.getMoyen().getType().name() : "INCONNU";
        String statutLabel = statut == StatutLigne.PAYE ? "PAYÉ intégralement"
                : "avance — restant : " + getMontantRestant();
        String note = versement.getDatePaiement().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + " : versé " + versement.getMontant() + " via " + typeMoyen
                + " [" + statutLabel + "]";
        commentaire = (commentaire == null || commentaire.isBlank())
                ? note : commentaire + " | " + note;
    }

    public BigDecimal getMontantRestant() {
        return montantDu.subtract(montantPaye).max(BigDecimal.ZERO);
    }

    public boolean estPayee() { return statut == StatutLigne.PAYE; }

    public UUID getId() { return id; }
    public TypeLigne getType() { return type; }
    public MoisAcademique getMoisAcademique() { return moisAcademique; }
    public int getOrdreReglement() { return ordreReglement; }
    public BigDecimal getMontantDu() { return montantDu; }
    public BigDecimal getMontantPaye() { return montantPaye; }
    public String getCommentaire() { return commentaire; }
    public StatutLigne getStatut() { return statut; }
    public List<Versement> getVersements() { return versements; }
    public void setStatut(StatutLigne statut) { this.statut = statut; }
}
