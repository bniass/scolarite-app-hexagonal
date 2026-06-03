package com.ecole221.inscription.service.domain.model;

import com.ecole221.common.entity.AggregateRoot;
import com.ecole221.inscription.service.domain.event.InscriptionCreeeEvent;
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
    /** true si l'étudiant a été créé lors de cette inscription (compensation = supprimer l'étudiant) */
    private boolean etudiantNouveau;

    private Inscription() {}

    public static Inscription creer(UUID etudiantId, boolean etudiantNouveau, UUID classeId, String codeAnnee,
            BigDecimal fraisInscription, BigDecimal mensualite, BigDecimal autresFrais,
            String moisAcademiquesJson, BigDecimal montantVerse, String typePaiement,
            String operateur, String referencePaiement, String nomBanque, String numeroTransaction) {
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
                fraisInscription, mensualite, autresFrais, moisAcademiquesJson,
                montantVerse, typePaiement, operateur, referencePaiement,
                nomBanque, numeroTransaction, i.creeLe
        ));
        return i;
    }

    public static Inscription reconstituer(UUID id, UUID etudiantId, boolean etudiantNouveau,
            UUID classeId, String codeAnnee, BigDecimal fraisInscription, BigDecimal mensualite,
            BigDecimal autresFrais, String moisAcademiquesJson, StatutInscription statut,
            LocalDateTime creeLe) {
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
}
