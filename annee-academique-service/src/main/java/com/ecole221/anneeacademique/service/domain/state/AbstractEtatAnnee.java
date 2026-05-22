package com.ecole221.anneeacademique.service.domain.state;


import com.ecole221.anneeacademique.service.domain.model.AnneeAcademique;
import com.ecole221.anneeacademique.service.domain.model.DatesAnnee;
import com.ecole221.anneeacademique.service.domain.exception.AnneeAcademiqueException;

public abstract class AbstractEtatAnnee implements EtatAnnee {

    protected void interdit(String action) {
        throw new AnneeAcademiqueException(
                "Action interdite : " + action + " dans l'état " + getClass().getSimpleName()
        );
    }

    @Override
    public void modifier(AnneeAcademique annee, DatesAnnee dates) {
        interdit("modifier dates");
    }

    @Override
    public void publier(AnneeAcademique annee) {
        interdit("publier");
    }

    @Override
    public void ouvrirInscriptions(AnneeAcademique annee) {
        interdit("ouvrir inscriptions");
    }

    @Override
    public void cloturer(AnneeAcademique annee) {
        interdit("clôturer");
    }

    @Override
    public void fermerInscriptions(AnneeAcademique annee) {
        interdit("fermer inscriptions");
    }
}

