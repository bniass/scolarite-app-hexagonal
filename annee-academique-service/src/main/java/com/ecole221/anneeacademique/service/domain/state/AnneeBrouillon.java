package com.ecole221.anneeacademique.service.domain.state;

import com.ecole221.anneeacademique.service.domain.model.AnneeAcademique;
import com.ecole221.anneeacademique.service.domain.model.DatesAnnee;

public class AnneeBrouillon extends AbstractEtatAnnee {

    @Override
    public void modifier(AnneeAcademique annee, DatesAnnee datesAnnee) {
        annee.changerEtat(new AnneeBrouillon());
    }

    @Override
    public void publier(AnneeAcademique annee) {
        annee.changerEtat(new AnneePubliee());
    }
}
