package com.ecole221.inscription.service.infrastructure.projection.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "annee_academique_projection")
public class AnneeAcademiqueProjectionJpaEntity {

    @Id
    @Column(length = 10)
    private String codeAnnee;

    @Column(length = 40)
    private String etatAnnee;

    protected AnneeAcademiqueProjectionJpaEntity() {
        // pour JPA uniquement
    }

    public AnneeAcademiqueProjectionJpaEntity(String codeAnnee, String etatAnnee) {
        this.codeAnnee = codeAnnee;
        this.etatAnnee = etatAnnee;
    }

    public String getCodeAnnee() {
        return codeAnnee;
    }

    public String getEtatAnnee() {
        return etatAnnee;
    }

    public void setEtatAnnee(String etatAnnee) {
        this.etatAnnee = etatAnnee;
    }
}