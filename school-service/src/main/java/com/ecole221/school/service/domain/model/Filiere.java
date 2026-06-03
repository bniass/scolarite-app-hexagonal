package com.ecole221.school.service.domain.model;

import com.ecole221.common.entity.AggregateRoot;
import com.ecole221.school.service.domain.event.FiliereCreeeEvent;
import com.ecole221.school.service.domain.exception.SchoolException;

import java.time.LocalDateTime;

public class Filiere extends AggregateRoot<FiliereId> {

    private String code;
    private String nom;

    public static Filiere creer(String code, String nom) {
        valider(code, nom);
        Filiere filiere = new Filiere();
        filiere.setId(FiliereId.generate());
        filiere.code = code.trim().toUpperCase();
        filiere.nom = nom.trim();
        filiere.addEvent(new FiliereCreeeEvent(
                filiere.getId().getValue().toString(),
                filiere.code,
                LocalDateTime.now()
        ));
        return filiere;
    }

    public static Filiere reconstituer(FiliereId id, String code, String nom) {
        Filiere filiere = new Filiere();
        filiere.setId(id);
        filiere.code = code;
        filiere.nom = nom;
        return filiere;
    }

    private static void valider(String code, String nom) {
        if (code == null || code.isBlank()) {
            throw new SchoolException("Le code de la filière est obligatoire");
        }
        if (nom == null || nom.isBlank()) {
            throw new SchoolException("Le nom de la filière est obligatoire");
        }
    }

    public String getCode() { return code; }
    public String getNom() { return nom; }
}
