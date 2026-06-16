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

    public InscriptionCreeeEvent(String inscriptionId, UUID etudiantId, UUID classeId,
            String codeAnnee, BigDecimal fraisInscription, BigDecimal mensualite,
            BigDecimal autresFrais, String moisAcademiquesJson, LocalDateTime occurredAt) {
        super(inscriptionId, occurredAt);
        this.etudiantId = etudiantId;
        this.classeId = classeId;
        this.codeAnnee = codeAnnee;
        this.fraisInscription = fraisInscription;
        this.mensualite = mensualite;
        this.autresFrais = autresFrais;
        this.moisAcademiquesJson = moisAcademiquesJson;
    }

    public UUID getEtudiantId() { return etudiantId; }
    public UUID getClasseId() { return classeId; }
    public String getCodeAnnee() { return codeAnnee; }
    public BigDecimal getFraisInscription() { return fraisInscription; }
    public BigDecimal getMensualite() { return mensualite; }
    public BigDecimal getAutresFrais() { return autresFrais; }
    public String getMoisAcademiquesJson() { return moisAcademiquesJson; }
}
