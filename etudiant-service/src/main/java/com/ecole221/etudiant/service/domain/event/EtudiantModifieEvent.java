package com.ecole221.etudiant.service.domain.event;

import com.ecole221.common.event.DomainEvent;
import com.ecole221.etudiant.service.domain.model.Etudiant;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class EtudiantModifieEvent implements DomainEvent<Etudiant> {

    private final UUID etudiantId;
    private final String matricule;
    private final String nom;
    private final String prenom;
    private final LocalDate dateNaissance;
    private final LocalDateTime occurredAt;

    public EtudiantModifieEvent(UUID etudiantId, String matricule, String nom,
                                 String prenom, LocalDate dateNaissance, LocalDateTime occurredAt) {
        this.etudiantId = etudiantId;
        this.matricule = matricule;
        this.nom = nom;
        this.prenom = prenom;
        this.dateNaissance = dateNaissance;
        this.occurredAt = occurredAt;
    }

    @Override
    public String aggregateId() { return etudiantId.toString(); }

    @Override
    public String aggregateType() { return "Etudiant"; }

    @Override
    public LocalDateTime occurredAt() { return occurredAt; }

    public UUID getEtudiantId() { return etudiantId; }
    public String getMatricule() { return matricule; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public LocalDate getDateNaissance() { return dateNaissance; }
}
