package com.ecole221.anneeacademique.service.domain.valuobject;

import com.ecole221.common.exception.DomainException;

public class CodeAnnee {
    private String codeAnnee;
    public CodeAnnee(Integer annee) {
        if(annee == null){
            throw new DomainException("Code année non crée!");
        }
        this.codeAnnee = String.format("%04d", annee)+"-"+String.format("%04d", (annee+1));
    }

    public String getCodeAnnee() {
        return codeAnnee;
    }
}
