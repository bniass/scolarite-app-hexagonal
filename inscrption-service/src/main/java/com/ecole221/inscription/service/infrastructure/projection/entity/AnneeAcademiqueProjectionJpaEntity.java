package com.ecole221.inscription.service.infrastructure.projection.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "annee_academique_projection")
public class AnneeAcademiqueProjectionJpaEntity {

    @Id
    @Column(length = 10)
    private String codeAnnee;

    @Column(length = 40)
    private String etatAnnee;

    @Lob
    @Column(name = "mois_academiques_json")
    private String moisAcademiquesJson = "[]";

    protected AnneeAcademiqueProjectionJpaEntity() {}

    public AnneeAcademiqueProjectionJpaEntity(String codeAnnee, String etatAnnee, String moisAcademiquesJson) {
        this.codeAnnee = codeAnnee;
        this.etatAnnee = etatAnnee;
        this.moisAcademiquesJson = moisAcademiquesJson != null ? moisAcademiquesJson : "[]";
    }

    public String getCodeAnnee() { return codeAnnee; }
    public String getEtatAnnee() { return etatAnnee; }
    public String getMoisAcademiquesJson() { return moisAcademiquesJson; }
    public void setEtatAnnee(String etatAnnee) { this.etatAnnee = etatAnnee; }
    public void setMoisAcademiquesJson(String json) { this.moisAcademiquesJson = json != null ? json : "[]"; }
}