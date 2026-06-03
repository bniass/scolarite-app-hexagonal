package com.ecole221.paiement.service.infrastructure.persistence.entity;

import com.ecole221.paiement.service.domain.valueobject.StatutDossier;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "dossier_paiement")
@Getter
@Setter
@NoArgsConstructor
public class DossierPaiementJpaEntity {

    @Id
    private UUID id;

    @Column(name = "inscription_id", nullable = false, unique = true)
    private UUID inscriptionId;

    @Column(name = "etudiant_id", nullable = false)
    private UUID etudiantId;

    @Column(name = "classe_id", nullable = false)
    private UUID classeId;

    @Column(name = "code_annee", nullable = false)
    private String codeAnnee;

    @Column(name = "frais_inscription", nullable = false, precision = 15, scale = 2)
    private BigDecimal fraisInscription;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal mensualite;

    @Column(name = "autres_frais", nullable = false, precision = 15, scale = 2)
    private BigDecimal autresFrais;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutDossier statut;

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<LignePaiementJpaEntity> lignes = new ArrayList<>();
}
