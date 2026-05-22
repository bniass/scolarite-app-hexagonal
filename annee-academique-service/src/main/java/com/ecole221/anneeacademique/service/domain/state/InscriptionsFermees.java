package com.ecole221.anneeacademique.service.domain.state;

import com.ecole221.anneeacademique.service.domain.model.AnneeAcademique;

public class InscriptionsFermees extends AbstractEtatAnnee{

    @Override
    public void cloturer(AnneeAcademique annee) {
        annee.changerEtat(new AnneeCloturee());
    }

}
