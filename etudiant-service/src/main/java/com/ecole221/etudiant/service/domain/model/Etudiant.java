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
    private String email;

    private Etudiant() {}

    public static Etudiant creer(String nom, String prenom, LocalDate dateNaissance,
                                  String email, long ordreInscription, String codeAnnee) {
        Etudiant etudiant = new Etudiant();
        etudiant.setId(new EtudiantId(UUID.randomUUID()));
        etudiant.matricule = new Matricule(genererMatricule(ordreInscription, codeAnnee));
        etudiant.nom = validerNom(nom);
        etudiant.prenom = validerPrenom(prenom);
        etudiant.dateNaissance = validerDateNaissance(dateNaissance);
        etudiant.email = validerEmail(email);

        etudiant.addEvent(new EtudiantCreeEvent(
                etudiant.getId().getValue(),
                etudiant.matricule.getValeur(),
                etudiant.nom,
                etudiant.prenom,
                etudiant.dateNaissance,
                etudiant.email,
                LocalDateTime.now()
        ));

        return etudiant;
    }

    public static Etudiant reconstituer(EtudiantId id, Matricule matricule,
                                         String nom, String prenom, LocalDate dateNaissance, String email) {
        Etudiant etudiant = new Etudiant();
        etudiant.setId(id);
        etudiant.matricule = matricule;
        etudiant.nom = nom;
        etudiant.prenom = prenom;
        etudiant.dateNaissance = dateNaissance;
        etudiant.email = email;
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

    // codeAnnee format: "2024-2025" → suffixe "2425"
    public static String suffixeAnnee(String codeAnnee) {
        String[] parts = codeAnnee.split("-");
        if (parts.length != 2 || parts[0].length() < 2 || parts[1].length() < 2) {
            throw new EtudiantException("Format du code année invalide : " + codeAnnee);
        }
        return parts[0].substring(2) + parts[1].substring(2);
    }

    private static String genererMatricule(long ordre, String codeAnnee) {
        return String.format("M%05d-%s", ordre, suffixeAnnee(codeAnnee));
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

    private static String validerEmail(String email) {
        if (email == null || email.isBlank()) throw new EtudiantException("L'email est obligatoire");
        String trimmed = email.trim().toLowerCase();
        if (!trimmed.contains("@")) throw new EtudiantException("Format d'email invalide");
        return trimmed;
    }

    public Matricule getMatricule() { return matricule; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public LocalDate getDateNaissance() { return dateNaissance; }
    public String getEmail() { return email; }
}
