package com.ecole221.etudiant.service.domain.model;

import com.ecole221.common.entity.AggregateRoot;
import com.ecole221.etudiant.service.domain.event.EtudiantCreeEvent;
import com.ecole221.etudiant.service.domain.event.EtudiantModifieEvent;
import com.ecole221.etudiant.service.domain.exception.EtudiantException;
import com.ecole221.etudiant.service.domain.valueobject.EtudiantId;
import com.ecole221.etudiant.service.domain.valueobject.Matricule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class Etudiant extends AggregateRoot<EtudiantId> {

    private Matricule matricule;
    private String nom;
    private String prenom;
    private LocalDate dateNaissance;

    private Etudiant() {}

    public static Etudiant creer(String matricule, String nom, String prenom, LocalDate dateNaissance) {
        Etudiant etudiant = new Etudiant();
        etudiant.setId(new EtudiantId(UUID.randomUUID()));
        etudiant.matricule = new Matricule(matricule);
        etudiant.nom = validerNom(nom);
        etudiant.prenom = validerPrenom(prenom);
        etudiant.dateNaissance = validerDateNaissance(dateNaissance);

        etudiant.addEvent(new EtudiantCreeEvent(
                etudiant.getId().getValue(),
                etudiant.matricule.getValeur(),
                etudiant.nom,
                etudiant.prenom,
                etudiant.dateNaissance,
                LocalDateTime.now()
        ));

        return etudiant;
    }

    public static Etudiant reconstituer(EtudiantId id, Matricule matricule,
                                         String nom, String prenom, LocalDate dateNaissance) {
        Etudiant etudiant = new Etudiant();
        etudiant.setId(id);
        etudiant.matricule = matricule;
        etudiant.nom = nom;
        etudiant.prenom = prenom;
        etudiant.dateNaissance = dateNaissance;
        return etudiant;
    }

    public void modifier(String nom, String prenom, LocalDate dateNaissance) {
        this.nom = validerNom(nom);
        this.prenom = validerPrenom(prenom);
        this.dateNaissance = validerDateNaissance(dateNaissance);

        addEvent(new EtudiantModifieEvent(
                this.getId().getValue(),
                this.matricule.getValeur(),
                this.nom,
                this.prenom,
                this.dateNaissance,
                LocalDateTime.now()
        ));
    }

    private static String validerNom(String nom) {
        if (nom == null || nom.isBlank()) throw new EtudiantException("Le nom est obligatoire");
        return nom.trim();
    }

    private static String validerPrenom(String prenom) {
        if (prenom == null || prenom.isBlank()) throw new EtudiantException("Le prénom est obligatoire");
        return prenom.trim();
    }

    private static LocalDate validerDateNaissance(LocalDate dateNaissance) {
        if (dateNaissance == null) throw new EtudiantException("La date de naissance est obligatoire");
        if (dateNaissance.isAfter(LocalDate.now())) throw new EtudiantException("La date de naissance ne peut pas être dans le futur");
        return dateNaissance;
    }

    public Matricule getMatricule() { return matricule; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public LocalDate getDateNaissance() { return dateNaissance; }
}
