package com.ecole221.paiement.service.application.port.in;

import com.ecole221.paiement.service.application.query.DossierResume;
import com.ecole221.paiement.service.application.query.LigneImpayee;
import com.ecole221.paiement.service.application.query.ResumeTableauBord;
import com.ecole221.paiement.service.domain.valueobject.StatutDossier;

import java.util.List;

public interface TableauBordUseCase {
    ResumeTableauBord resume(String codeAnnee);
    List<DossierResume> dossiers(String codeAnnee, StatutDossier statut, int page, int size);
    long totalDossiers(String codeAnnee, StatutDossier statut);
    List<LigneImpayee> impayes(String codeAnnee, int page, int size);
    long totalImpayes(String codeAnnee);
}
