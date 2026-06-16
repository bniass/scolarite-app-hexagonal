package com.ecole221.inscription.service.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class InscriptionTransfereEvent extends InscriptionEvent {

    private final UUID etudiantId;
    private final UUID nouvelleClasseId;
    private final String codeAnnee;
    private final BigDecimal fraisInscription;
    private final BigDecimal mensualite;
    private final BigDecimal autresFrais;
    private final String moisAcademiquesJson;

    public InscriptionTransfereEvent(String inscriptionId, UUID etudiantId, UUID nouvelleClasseId,
            String codeAnnee, BigDecimal fraisInscription, BigDecimal mensualite,
            BigDecimal autresFrais, String moisAcademiquesJson, LocalDateTime occurredAt) {
        super(inscriptionId, occurredAt);
        this.etudiantId = etudiantId;
        this.nouvelleClasseId = nouvelleClasseId;
        this.codeAnnee = codeAnnee;
        this.fraisInscription = fraisInscription;
        this.mensualite = mensualite;
        this.autresFrais = autresFrais;
        this.moisAcademiquesJson = moisAcademiquesJson;
    }

    public UUID getEtudiantId() { return etudiantId; }
    public UUID getNouvelleClasseId() { return nouvelleClasseId; }
    public String getCodeAnnee() { return codeAnnee; }
    public BigDecimal getFraisInscription() { return fraisInscription; }
    public BigDecimal getMensualite() { return mensualite; }
    public BigDecimal getAutresFrais() { return autresFrais; }
    public String getMoisAcademiquesJson() { return moisAcademiquesJson; }
}
