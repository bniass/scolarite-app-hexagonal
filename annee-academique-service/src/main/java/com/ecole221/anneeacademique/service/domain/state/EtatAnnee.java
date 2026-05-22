package com.ecole221.anneeacademique.service.domain.state;

import com.ecole221.anneeacademique.service.domain.model.AnneeAcademique;
import com.ecole221.anneeacademique.service.domain.model.DatesAnnee;

public interface EtatAnnee {
    void modifier(AnneeAcademique annee, DatesAnnee dates);
    void publier(AnneeAcademique annee);
    void ouvrirInscriptions(AnneeAcademique annee);
    void fermerInscriptions(AnneeAcademique annee);
    void cloturer(AnneeAcademique annee);
}

