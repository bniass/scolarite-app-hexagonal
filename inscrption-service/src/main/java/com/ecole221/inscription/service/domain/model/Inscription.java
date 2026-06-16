package com.ecole221.inscription.service.domain.model;

import com.ecole221.common.entity.AggregateRoot;
import com.ecole221.inscription.service.domain.event.InscriptionAnnuleeEvent;
import com.ecole221.inscription.service.domain.event.InscriptionCreeeEvent;
import com.ecole221.inscription.service.domain.event.InscriptionTransfereEvent;
import com.ecole221.inscription.service.domain.exception.InscriptionException;
import com.ecole221.inscription.service.domain.valueobject.StatutInscription;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Inscription extends AggregateRoot<UUID> {

    private UUID etudiantId;
    private UUID classeId;
    private String codeAnnee;
    private BigDecimal fraisInscription;
    private BigDecimal mensualite;
    private BigDecimal autresFrais;
    private String moisAcademiquesJson;
    private StatutInscription statut;
    private LocalDateTime creeLe;
    private boolean etudiantNouveau;
    private LocalDateTime annuleLe;
    private String motifAnnulation;

    private Inscription() {}

    public static Inscription creer(UUID etudiantId, boolean etudiantNouveau, UUID classeId,
            String codeAnnee, BigDecimal fraisInscription, BigDecimal mensualite,
            BigDecimal autresFrais, String moisAcademiquesJson) {
        Inscription i = new Inscription();
        i.setId(UUID.randomUUID());
        i.etudiantId = etudiantId;
        i.etudiantNouveau = etudiantNouveau;
        i.classeId = classeId;
        i.codeAnnee = codeAnnee;
        i.fraisInscription = fraisInscription;
        i.mensualite = mensualite;
        i.autresFrais = autresFrais;
        i.moisAcademiquesJson = moisAcademiquesJson;
        i.statut = StatutInscription.PENDING;
        i.creeLe = LocalDateTime.now();

        i.addEvent(new InscriptionCreeeEvent(
                i.getId().toString(), etudiantId, classeId, codeAnnee,
                fraisInscription, mensualite, autresFrais, moisAcademiquesJson, i.creeLe
        ));
        return i;
    }

    public static Inscription reconstituer(UUID id, UUID etudiantId, boolean etudiantNouveau,
            UUID classeId, String codeAnnee, BigDecimal fraisInscription, BigDecimal mensualite,
            BigDecimal autresFrais, String moisAcademiquesJson, StatutInscription statut,
            LocalDateTime creeLe, LocalDateTime annuleLe, String motifAnnulation) {
        Inscription i = new Inscription();
        i.setId(id);
        i.etudiantId = etudiantId;
        i.etudiantNouveau = etudiantNouveau;
        i.classeId = classeId;
        i.codeAnnee = codeAnnee;
        i.fraisInscription = fraisInscription;
        i.mensualite = mensualite;
        i.autresFrais = autresFrais;
        i.moisAcademiquesJson = moisAcademiquesJson;
        i.statut = statut;
        i.creeLe = creeLe;
        i.annuleLe = annuleLe;
        i.motifAnnulation = motifAnnulation;
        return i;
    }

    public void confirmer() {
        if (statut != StatutInscription.PENDING) {
            throw new InscriptionException("Impossible de confirmer une inscription en statut " + statut);
        }
        statut = StatutInscription.CONFIRMEE;
    }

    public void echouer() {
        if (statut != StatutInscription.PENDING) {
            throw new InscriptionException("Impossible d'échouer une inscription en statut " + statut);
        }
        statut = StatutInscription.ECHOUEE;
    }

    public void annuler(String motif) {
        if (statut == StatutInscription.CONFIRMEE) {
            throw new InscriptionException(
                    "Impossible d'annuler une inscription ayant au moins un versement (statut : CONFIRMEE)");
        }
        if (statut == StatutInscription.ANNULEE) {
            throw new InscriptionException("L'inscription est déjà annulée");
        }
        if (statut != StatutInscription.PENDING) {
            throw new InscriptionException(
                    "Impossible d'annuler une inscription en statut " + statut);
        }
        statut = StatutInscription.ANNULEE;
        annuleLe = LocalDateTime.now();
        motifAnnulation = motif;
        addEvent(new InscriptionAnnuleeEvent(getId().toString(), annuleLe));
    }

    public void changerClasse(UUID nouvelleClasseId, BigDecimal nouveauxFraisInscription,
            BigDecimal nouvelleMensualite, BigDecimal nouveauxAutresFrais,
            String nouveauxMoisAcademiquesJson, String niveauNouvelleClasse, int delaiMaxMois) {

        if (statut != StatutInscription.CONFIRMEE) {
            throw new InscriptionException(
                    "Le transfert de classe n'est possible que pour une inscription confirmée (statut actuel : " + statut + ")");
        }
        LocalDateTime limite = creeLe.plusMonths(delaiMaxMois);
        if (LocalDateTime.now().isAfter(limite)) {
            throw new InscriptionException(
                    "Le délai de transfert de classe (" + delaiMaxMois + " mois) est dépassé");
        }
        if (niveauNouvelleClasse == null || (!niveauNouvelleClasse.equals("L1") && !niveauNouvelleClasse.equals("M1"))) {
            throw new InscriptionException(
                    "Le transfert n'est autorisé que vers une classe de niveau L1 ou M1 (niveau demandé : " + niveauNouvelleClasse + ")");
        }

        classeId = nouvelleClasseId;
        fraisInscription = nouveauxFraisInscription;
        mensualite = nouvelleMensualite;
        autresFrais = nouveauxAutresFrais;
        moisAcademiquesJson = nouveauxMoisAcademiquesJson;

        addEvent(new InscriptionTransfereEvent(
                getId().toString(), etudiantId, nouvelleClasseId, codeAnnee,
                fraisInscription, mensualite, autresFrais, moisAcademiquesJson,
                LocalDateTime.now()));
    }

    public UUID getEtudiantId() { return etudiantId; }
    public boolean isEtudiantNouveau() { return etudiantNouveau; }
    public UUID getClasseId() { return classeId; }
    public String getCodeAnnee() { return codeAnnee; }
    public BigDecimal getFraisInscription() { return fraisInscription; }
    public BigDecimal getMensualite() { return mensualite; }
    public BigDecimal getAutresFrais() { return autresFrais; }
    public String getMoisAcademiquesJson() { return moisAcademiquesJson; }
    public StatutInscription getStatut() { return statut; }
    public LocalDateTime getCreeLe() { return creeLe; }
    public LocalDateTime getAnnuleLe() { return annuleLe; }
    public String getMotifAnnulation() { return motifAnnulation; }
}
