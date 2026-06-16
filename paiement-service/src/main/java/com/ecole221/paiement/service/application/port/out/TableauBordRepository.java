package com.ecole221.paiement.service.application.port.out;

import com.ecole221.paiement.service.application.query.DossierResume;
import com.ecole221.paiement.service.application.query.LigneImpayee;
import com.ecole221.paiement.service.application.query.ResumeTableauBord;
import com.ecole221.paiement.service.domain.valueobject.StatutDossier;

import java.util.List;

public interface TableauBordRepository {
    ResumeTableauBord resume(String codeAnnee);
    List<DossierResume> listerDossiers(String codeAnnee, StatutDossier statut, int page, int size);
    long compterDossiers(String codeAnnee, StatutDossier statut);
    List<LigneImpayee> listerImpayes(String codeAnnee, int page, int size);
    long compterImpayes(String codeAnnee);
}
