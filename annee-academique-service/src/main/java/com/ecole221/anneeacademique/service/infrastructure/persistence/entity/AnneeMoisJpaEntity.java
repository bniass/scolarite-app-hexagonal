package com.ecole221.anneeacademique.service.infrastructure.persistence.entity;

import jakarta.persistence.*;


@Entity
@Table(
        name = "annee_mois",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"annee_id", "mois", "annee"}
                )
        }
)
public class AnneeMoisJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annee_id", nullable = false)
    private AnneeAcademiqueJpaEntity anneeAcademique;

    @Column(nullable = false)
    private int mois;

    @Column(nullable = false)
    private int annee;

    protected AnneeMoisJpaEntity() {}

    public AnneeMoisJpaEntity(AnneeAcademiqueJpaEntity anneeAcademique, int mois, int annee) {
        this.anneeAcademique = anneeAcademique;
        this.mois = mois;
        this.annee = annee;
    }

    public Long getId() {
        return id;
    }

    public int getMois() {
        return mois;
    }

    public int getAnnee() {
        return annee;
    }
}
