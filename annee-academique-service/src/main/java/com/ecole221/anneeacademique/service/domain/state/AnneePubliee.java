package com.ecole221.anneeacademique.service.domain.state;

import com.ecole221.anneeacademique.service.domain.model.AnneeAcademique;

public class AnneePubliee extends AbstractEtatAnnee{

    @Override
    public void ouvrirInscriptions(AnneeAcademique annee) {
        annee.changerEtat(new InscriptionsOuvertes());
    }
}
