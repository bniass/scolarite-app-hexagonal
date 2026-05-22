package com.ecole221.anneeacademique.service.infrastructure.persistence.mapper;


import com.ecole221.anneeacademique.service.domain.model.Statut;
import com.ecole221.anneeacademique.service.domain.state.*;

public final class EtatAnneeFactory {

    private EtatAnneeFactory() {}

    public static EtatAnnee fromStatut(Statut statut) {
        return switch (statut) {
            case BROUILLON -> new AnneeBrouillon();
            case PUBLIEE -> new AnneePubliee();
            case INSCRIPTIONS_OUVERTES -> new InscriptionsOuvertes();
            case INSCRIPTIONS_FERMEES -> new InscriptionsFermees();
            case CLOTUREE -> new AnneeCloturee();
        };
    }

    public static Statut toStatut(EtatAnnee etat) {
        if (etat instanceof AnneeBrouillon) return Statut.BROUILLON;
        if (etat instanceof AnneePubliee) return Statut.PUBLIEE;
        if (etat instanceof InscriptionsOuvertes) return Statut.INSCRIPTIONS_OUVERTES;
        if (etat instanceof InscriptionsFermees) return Statut.INSCRIPTIONS_FERMEES;
        if (etat instanceof AnneeCloturee) return Statut.CLOTUREE;

        throw new IllegalStateException("Etat inconnu : " + etat.getClass());
    }
}

