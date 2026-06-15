package com.ecole221.etudiant.service.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "matricule_sequence")
public class MatriculeSequenceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "suffixe_annee", nullable = false, length = 10)
    private String suffixeAnnee;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public MatriculeSequenceJpaEntity() {}

    public MatriculeSequenceJpaEntity(String suffixeAnnee) {
        this.suffixeAnnee = suffixeAnnee;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getSuffixeAnnee() { return suffixeAnnee; }
}
