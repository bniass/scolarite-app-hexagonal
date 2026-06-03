package com.ecole221.school.service.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "classe_tarif")
public class ClasseTarifJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classe_id", nullable = false)
    private ClasseJpaEntity classe;

    @Column(name = "tarif_id", nullable = false)
    private UUID tarifId;

    @Column(nullable = false)
    private LocalDate dateActivation;

    private LocalDate dateDesactivation;

    @Column(nullable = false)
    private boolean actif;

    public ClasseTarifJpaEntity() {}

    public ClasseTarifJpaEntity(ClasseJpaEntity classe, UUID tarifId, LocalDate dateActivation, boolean actif) {
        this.classe = classe;
        this.tarifId = tarifId;
        this.dateActivation = dateActivation;
        this.actif = actif;
    }

    public Long getId() { return id; }
    public ClasseJpaEntity getClasse() { return classe; }
    public void setClasse(ClasseJpaEntity classe) { this.classe = classe; }
    public UUID getTarifId() { return tarifId; }
    public void setTarifId(UUID tarifId) { this.tarifId = tarifId; }
    public LocalDate getDateActivation() { return dateActivation; }
    public void setDateActivation(LocalDate dateActivation) { this.dateActivation = dateActivation; }
    public LocalDate getDateDesactivation() { return dateDesactivation; }
    public void setDateDesactivation(LocalDate dateDesactivation) { this.dateDesactivation = dateDesactivation; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
}
