package com.ecole221.school.service.infrastructure.persistence.entity;

import com.ecole221.school.service.domain.model.Cycle;
import com.ecole221.school.service.domain.model.Niveau;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "classe")
public class ClasseJpaEntity {

    @Id
    @Column(nullable = false, unique = true)
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Cycle cycle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Niveau niveau;

    @Column(name = "filiere_id", nullable = false)
    private UUID filiereId;

    @OneToMany(
            mappedBy = "classe",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<ClasseTarifJpaEntity> historiqueTarifs = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public Cycle getCycle() { return cycle; }
    public void setCycle(Cycle cycle) { this.cycle = cycle; }
    public Niveau getNiveau() { return niveau; }
    public void setNiveau(Niveau niveau) { this.niveau = niveau; }
    public UUID getFiliereId() { return filiereId; }
    public void setFiliereId(UUID filiereId) { this.filiereId = filiereId; }
    public List<ClasseTarifJpaEntity> getHistoriqueTarifs() { return historiqueTarifs; }
    public void setHistoriqueTarifs(List<ClasseTarifJpaEntity> historiqueTarifs) { this.historiqueTarifs = historiqueTarifs; }
}
