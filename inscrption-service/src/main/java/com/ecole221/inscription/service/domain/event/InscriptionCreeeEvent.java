package com.ecole221.inscription.service.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class InscriptionCreeeEvent extends InscriptionEvent {
    private final UUID etudiantId;
    private final UUID classeId;
    private final String codeAnnee;
    private final BigDecimal fraisInscription;
    private final BigDecimal mensualite;
    private final BigDecimal autresFrais;
    private final String moisAcademiquesJson;
    private final BigDecimal montantVerse;
    private final String typePaiement;
    private final String operateur;
    private final String referencePaiement;
    private final String nomBanque;
    private final String numeroTransaction;

    public InscriptionCreeeEvent(String inscriptionId, UUID etudiantId, UUID classeId,
            String codeAnnee, BigDecimal fraisInscription, BigDecimal mensualite,
            BigDecimal autresFrais, String moisAcademiquesJson,
            BigDecimal montantVerse, String typePaiement,
            String operateur, String referencePaiement,
            String nomBanque, String numeroTransaction,
            LocalDateTime occurredAt) {
        super(inscriptionId, occurredAt);
        this.etudiantId = etudiantId;
        this.classeId = classeId;
        this.codeAnnee = codeAnnee;
        this.fraisInscription = fraisInscription;
        this.mensualite = mensualite;
        this.autresFrais = autresFrais;
        this.moisAcademiquesJson = moisAcademiquesJson;
        this.montantVerse = montantVerse;
        this.typePaiement = typePaiement;
        this.operateur = operateur != null ? operateur : "";
        this.referencePaiement = referencePaiement != null ? referencePaiement : "";
        this.nomBanque = nomBanque != null ? nomBanque : "";
        this.numeroTransaction = numeroTransaction != null ? numeroTransaction : "";
    }

    public UUID getEtudiantId() { return etudiantId; }
    public UUID getClasseId() { return classeId; }
    public String getCodeAnnee() { return codeAnnee; }
    public BigDecimal getFraisInscription() { return fraisInscription; }
    public BigDecimal getMensualite() { return mensualite; }
    public BigDecimal getAutresFrais() { return autresFrais; }
    public String getMoisAcademiquesJson() { return moisAcademiquesJson; }
    public BigDecimal getMontantVerse() { return montantVerse; }
    public String getTypePaiement() { return typePaiement; }
    public String getOperateur() { return operateur; }
    public String getReferencePaiement() { return referencePaiement; }
    public String getNomBanque() { return nomBanque; }
    public String getNumeroTransaction() { return numeroTransaction; }
}
