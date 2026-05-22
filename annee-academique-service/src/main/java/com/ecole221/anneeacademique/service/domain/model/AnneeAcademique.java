package com.ecole221.anneeacademique.service.domain.model;


import com.ecole221.anneeacademique.service.domain.event.AnneeAcademiqueCreeeEvent;
import com.ecole221.anneeacademique.service.domain.exception.AnneeAcademiqueException;
import com.ecole221.anneeacademique.service.domain.state.*;
import com.ecole221.anneeacademique.service.domain.valuobject.CodeAnnee;
import com.ecole221.anneeacademique.service.infrastructure.persistence.StatutAnneeAcademique;
import com.ecole221.common.entity.AggregateRoot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AnneeAcademique extends AggregateRoot<AnneeAcademiqueId> {
    private static final int DUREE_ANNEE_SCOLAIRE_MOIS = 9;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private LocalDate dateOuvertureInscription;
    private LocalDate dateFinInscription;
    private LocalDate datePublication;
    private List<MoisAcademique> moisAcademiques;

    private EtatAnnee etatAnnee;

    public static AnneeAcademique reconstituer(String code, DatesAnnee datesAnnee, LocalDate datePublication, EtatAnnee etat, List<MoisAcademique> mois) {
        code = code.substring(0, 4);
        AnneeAcademiqueId id = new AnneeAcademiqueId(
                new CodeAnnee(Integer.parseInt(code))
        );
        AnneeAcademique annee = new AnneeAcademique();
        annee.setId(id);
        annee.dateDebut = datesAnnee.getDateDebut();
        annee.dateFin = datesAnnee.getDateFin();
        annee.dateOuvertureInscription = datesAnnee.getDateOuvertureInscription();
        annee.dateFinInscription = datesAnnee.getDateFinInscription();
        annee.datePublication = datePublication;
        annee.etatAnnee = etat;
        annee.moisAcademiques = mois;
        return annee;
    }

    //creér année
    public static AnneeAcademique creer(int annee, DatesAnnee datesAnnee) {
        checkDates(datesAnnee);
        checkIfDureAnnneEstValide(datesAnnee.getDateDebut(), datesAnnee.getDateFin());
        AnneeAcademique anneeAcademique = new AnneeAcademique();
        anneeAcademique.setId(new AnneeAcademiqueId(new CodeAnnee(annee)));
        anneeAcademique.setData(anneeAcademique, datesAnnee);
        anneeAcademique.addEvent(
                new AnneeAcademiqueCreeeEvent(
                        anneeAcademique.getId().getValue().getCodeAnnee(),
                        StatutAnneeAcademique.BROUILLON.name(),
                        LocalDateTime.now()
                )
        );
        return anneeAcademique;
    }



    //modifier année
    public void modifier(DatesAnnee datesAnnee){
        checkDates(datesAnnee);
        checkIfDureAnnneEstValide(datesAnnee.getDateDebut(), datesAnnee.getDateFin());
        setData(this, datesAnnee);
        etatAnnee.modifier(this, datesAnnee);
        this.addEvent(
                new AnneeAcademiqueCreeeEvent(
                        this.getId().getValue().getCodeAnnee(),
                        StatutAnneeAcademique.BROUILLON.name(),
                        LocalDateTime.now()
                )
        );
    }

    private void setData(AnneeAcademique anneeAcademique, DatesAnnee datesAnnee)
    {
        anneeAcademique.dateDebut = datesAnnee.getDateDebut();
        anneeAcademique.dateFin = datesAnnee.getDateFin();
        anneeAcademique.dateOuvertureInscription = datesAnnee.getDateOuvertureInscription();
        anneeAcademique.dateFinInscription = datesAnnee.getDateFinInscription();
        anneeAcademique.changerEtat(new AnneeBrouillon());
        anneeAcademique.moisAcademiques = genererMoisAcademiques(datesAnnee.getDateDebut());
    }

    //publier année
    public void publier(){
        datePublication = LocalDate.now();
        etatAnnee.publier(this);
        this.addEvent(
                new AnneeAcademiqueCreeeEvent(
                        this.getId().getValue().getCodeAnnee(),
                        StatutAnneeAcademique.PUBLIEE.name(),
                        LocalDateTime.now()
                )
        );
    }
    //ouvrir inscription
    public void ouvrirInscription(){
        etatAnnee.ouvrirInscriptions(this);
        this.addEvent(
                new AnneeAcademiqueCreeeEvent(
                        this.getId().getValue().getCodeAnnee(),
                        StatutAnneeAcademique.INSCRIPTIONS_OUVERTES.name(),
                        LocalDateTime.now()
                )
        );
    }
    //cloturer inscription
    public void cloturerInscription(){
        etatAnnee.fermerInscriptions(this);
        this.addEvent(
                new AnneeAcademiqueCreeeEvent(
                        this.getId().getValue().getCodeAnnee(),
                        StatutAnneeAcademique.INSCRIPTIONS_FERMEES.name(),
                        LocalDateTime.now()
                )
        );
    }
    //cloturer année
    public void cloturer(){
        etatAnnee.cloturer(this);
        this.addEvent(
                new AnneeAcademiqueCreeeEvent(
                        this.getId().getValue().getCodeAnnee(),
                        StatutAnneeAcademique.CLOTUREE.name(),
                        LocalDateTime.now()
                )
        );
    }

    public void changerEtat(EtatAnnee etatAnnee) {
        this.etatAnnee = etatAnnee;
    }

    //vérifier cohérence des dates
    private static void checkDates(DatesAnnee datesAnnee){
        Objects.requireNonNull(datesAnnee, "Les dates sont obligatoires");

        LocalDate debut = datesAnnee.getDateDebut();
        LocalDate fin = datesAnnee.getDateFin();
        LocalDate ouverture = datesAnnee.getDateOuvertureInscription();
        LocalDate finInscription = datesAnnee.getDateFinInscription();

        // Vérification null
        if (debut == null || fin == null || ouverture == null || finInscription == null) {
            throw new AnneeAcademiqueException("Toutes les dates doivent être renseignées");
        }

        // 1. Année académique
        if (!debut.isBefore(fin)) {
            throw new AnneeAcademiqueException(
                    "La date de début doit être strictement antérieure à la date de fin"
            );
        }

        // 2. Période d’inscription
        if (!ouverture.isBefore(finInscription)) {
            throw new AnneeAcademiqueException(
                    "La date d’ouverture doit être antérieure à la date de fin des inscriptions"
            );
        }

        // 3. Cohérence globale
        if (ouverture.isAfter(debut)) {
            throw new AnneeAcademiqueException(
                    "L’ouverture des inscriptions doit être avant ou égale au début de l’année"
            );
        }

        if (!finInscription.isAfter(debut)) {
            throw new AnneeAcademiqueException(
                    "La fin des inscriptions doit être après le début de l’année"
            );
        }

        if (finInscription.isAfter(fin)) {
            throw new AnneeAcademiqueException(
                    "La fin des inscriptions doit être avant ou égale à la fin de l’année"
            );
        }
    }

    //verifier si l'annéé scolaire fait 9 mois
    private static void checkIfDureAnnneEstValide(LocalDate dateDebut, LocalDate dateFin){
        long mois = ChronoUnit.MONTHS.between(
                dateDebut.withDayOfMonth(1),
                dateFin.withDayOfMonth(1)
        );

        if (dateDebut.plusMonths(mois).isBefore(dateFin)) {
            mois++;
        }

        if (mois != DUREE_ANNEE_SCOLAIRE_MOIS) {
            throw new AnneeAcademiqueException(
                    "Une année scolaire doit couvrir exactement "
                            + DUREE_ANNEE_SCOLAIRE_MOIS + " mois"
            );
        }
    }

    //générer les mois académiques
    public static List<MoisAcademique> genererMoisAcademiques(LocalDate dateDebut) {
        List<MoisAcademique> mois = new ArrayList<>();
        LocalDate courant = dateDebut.withDayOfMonth(1);

        for (int i = 0; i < DUREE_ANNEE_SCOLAIRE_MOIS; i++) {
            mois.add(new MoisAcademique(courant.getMonthValue(), courant.getYear()));
            courant = courant.plusMonths(1);
        }
        return mois;
    }


    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public LocalDate getDateOuvertureInscription() {
        return dateOuvertureInscription;
    }

    public LocalDate getDateFinInscription() {
        return dateFinInscription;
    }

    public LocalDate getDatePublication() {
        return datePublication;
    }

    public List<MoisAcademique> getMoisAcademiques() {
        return moisAcademiques;
    }

    public EtatAnnee getEtatAnnee() {
        return etatAnnee;
    }
}
