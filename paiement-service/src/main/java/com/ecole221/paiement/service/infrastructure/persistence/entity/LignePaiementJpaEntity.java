package com.ecole221.paiement.service.infrastructure.persistence.entity;

import com.ecole221.paiement.service.domain.valueobject.StatutLigne;
import com.ecole221.paiement.service.domain.valueobject.TypeLigne;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ligne_paiement")
@Getter
@Setter
@NoArgsConstructor
public class LignePaiementJpaEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dossier_id", nullable = false)
    private DossierPaiementJpaEntity dossier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeLigne type;

    @Column(name = "mois_academique_mois")
    private Integer moisAcademiqueMois;

    @Column(name = "mois_academique_annee")
    private Integer moisAcademiqueAnnee;

    @Column(name = "ordre_reglement", nullable = false)
    private int ordreReglement;

    @Column(name = "montant_du", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantDu;

    @Column(name = "montant_paye", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantPaye;

    @Column(nullable = false)
    private String commentaire;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutLigne statut;

    @OneToMany(mappedBy = "ligne", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<VersementJpaEntity> versements = new ArrayList<>();
}
