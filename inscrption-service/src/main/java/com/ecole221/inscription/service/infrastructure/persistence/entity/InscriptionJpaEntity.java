package com.ecole221.inscription.service.infrastructure.persistence.entity;

import com.ecole221.inscription.service.domain.valueobject.StatutInscription;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inscription")
@Getter
@Setter
@NoArgsConstructor
public class InscriptionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "etudiant_id", nullable = false)
    private UUID etudiantId;

    @Column(name = "classe_id", nullable = false)
    private UUID classeId;

    @Column(name = "code_annee", nullable = false, length = 20)
    private String codeAnnee;

    @Column(name = "frais_inscription", nullable = false, precision = 15, scale = 2)
    private BigDecimal fraisInscription;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal mensualite;

    @Column(name = "autres_frais", nullable = false, precision = 15, scale = 2)
    private BigDecimal autresFrais;

    @Lob
    @Column(name = "mois_academiques_json")
    private String moisAcademiquesJson = "[]";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutInscription statut;

    @Column(name = "etudiant_nouveau", nullable = false)
    private boolean etudiantNouveau;

    @Column(name = "cree_le", nullable = false)
    private LocalDateTime creeLe;

    @Column(name = "annule_le")
    private LocalDateTime annuleLe;

    @Column(name = "motif_annulation", length = 500)
    private String motifAnnulation;
}
