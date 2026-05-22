package com.ecole221.inscription.service.domain.model.projection;

import com.ecole221.inscription.service.domain.valueobject.CodeAnnee;
import com.ecole221.inscription.service.domain.valueobject.EtatAnnee;

public class AnneeAcademiqueProjection {

    private final CodeAnnee codeAnnee;
    private EtatAnnee etatAnnee;

    public AnneeAcademiqueProjection(CodeAnnee codeAnnee, EtatAnnee etatAnnee) {
        if (codeAnnee == null) {
            throw new IllegalArgumentException("Le code année est obligatoire");
        }
        if (etatAnnee == null) {
            throw new IllegalArgumentException("L'état de l'année est obligatoire");
        }

        this.codeAnnee = codeAnnee;
        this.etatAnnee = etatAnnee;
    }

    public void changerEtat(EtatAnnee nouvelEtat) {
        if (nouvelEtat == null) {
            throw new IllegalArgumentException("Le nouvel état est obligatoire");
        }
        this.etatAnnee = nouvelEtat;
    }

    public CodeAnnee getCodeAnnee() {
        return codeAnnee;
    }

    public EtatAnnee getEtatAnnee() {
        return etatAnnee;
    }
}