package com.ecole221.anneeacademique.service.infrastructure.persistence.entity;

import com.ecole221.anneeacademique.service.domain.model.Statut;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "annee_academique")
public class AnneeAcademiqueJpaEntity {

    @Id
    @Column(nullable = false, unique = true, length = 12)
    private String code;

    private LocalDate dateDebut;
    private LocalDate dateFin;
    private LocalDate dateDebutInscriptions;
    private LocalDate dateFinInscriptions;

    @Enumerated(EnumType.STRING)
    private Statut statut;

    private LocalDate datePublication;

    /* =======================
       MOIS ACADEMIQUES
       ======================= */

    @OneToMany(
            mappedBy = "anneeAcademique",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<AnneeMoisJpaEntity> moisAcademiques = new ArrayList<>();
}
