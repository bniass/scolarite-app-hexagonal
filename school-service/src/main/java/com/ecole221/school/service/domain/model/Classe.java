package com.ecole221.school.service.domain.model;

import com.ecole221.common.entity.AggregateRoot;
import com.ecole221.school.service.domain.event.ClasseCreeeEvent;
import com.ecole221.school.service.domain.event.TarifAffecteEvent;
import com.ecole221.school.service.domain.exception.SchoolException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Classe extends AggregateRoot<ClasseId> {

    private String code;
    private String nom;
    private Cycle cycle;
    private Niveau niveau;
    private FiliereId filiereId;
    private List<ClasseTarif> historiqueTarifs;

    public static Classe creer(String code, String nom, Cycle cycle, Niveau niveau, FiliereId filiereId) {
        valider(code, nom, cycle, niveau, filiereId);
        validerCycleNiveau(cycle, niveau);
        Classe classe = new Classe();
        classe.setId(ClasseId.generate());
        classe.code = code.trim().toUpperCase();
        classe.nom = nom.trim();
        classe.cycle = cycle;
        classe.niveau = niveau;
        classe.filiereId = filiereId;
        classe.historiqueTarifs = new ArrayList<>();
        classe.addEvent(new ClasseCreeeEvent(
                classe.getId().getValue().toString(),
                classe.code,
                LocalDateTime.now()
        ));
        return classe;
    }

    public static Classe reconstituer(ClasseId id, String code, String nom, Cycle cycle,
                                      Niveau niveau, FiliereId filiereId, List<ClasseTarif> historiqueTarifs) {
        Classe classe = new Classe();
        classe.setId(id);
        classe.code = code;
        classe.nom = nom;
        classe.cycle = cycle;
        classe.niveau = niveau;
        classe.filiereId = filiereId;
        classe.historiqueTarifs = new ArrayList<>(historiqueTarifs);
        return classe;
    }

    public void affecterTarif(TarifId tarifId) {
        Objects.requireNonNull(tarifId, "Le tarif est obligatoire");
        this.historiqueTarifs.stream()
                .filter(ClasseTarif::isActif)
                .forEach(ct -> ct.desactiver(LocalDate.now()));
        this.historiqueTarifs.add(new ClasseTarif(tarifId, LocalDate.now()));
        this.addEvent(new TarifAffecteEvent(
                this.getId().getValue().toString(),
                tarifId.getValue().toString(),
                LocalDateTime.now()
        ));
    }

    public ClasseTarif getTarifActif() {
        return historiqueTarifs.stream()
                .filter(ClasseTarif::isActif)
                .findFirst()
                .orElse(null);
    }

    private static void valider(String code, String nom, Cycle cycle, Niveau niveau, FiliereId filiereId) {
        if (code == null || code.isBlank()) {
            throw new SchoolException("Le code de la classe est obligatoire");
        }
        if (nom == null || nom.isBlank()) {
            throw new SchoolException("Le nom de la classe est obligatoire");
        }
        if (cycle == null) {
            throw new SchoolException("Le cycle est obligatoire");
        }
        if (niveau == null) {
            throw new SchoolException("Le niveau est obligatoire");
        }
        if (filiereId == null) {
            throw new SchoolException("La filière est obligatoire");
        }
    }

    private static void validerCycleNiveau(Cycle cycle, Niveau niveau) {
        if (cycle == Cycle.LICENCE && (niveau == Niveau.M1 || niveau == Niveau.M2)) {
            throw new SchoolException("Un niveau Master ne peut pas être associé au cycle Licence");
        }
        if (cycle == Cycle.MASTER && (niveau == Niveau.L1 || niveau == Niveau.L2 || niveau == Niveau.L3)) {
            throw new SchoolException("Un niveau Licence ne peut pas être associé au cycle Master");
        }
    }

    public String getCode() { return code; }
    public String getNom() { return nom; }
    public Cycle getCycle() { return cycle; }
    public Niveau getNiveau() { return niveau; }
    public FiliereId getFiliereId() { return filiereId; }
    public List<ClasseTarif> getHistoriqueTarifs() { return List.copyOf(historiqueTarifs); }
}
