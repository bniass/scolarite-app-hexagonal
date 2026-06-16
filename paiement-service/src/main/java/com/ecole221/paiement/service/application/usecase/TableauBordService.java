package com.ecole221.paiement.service.application.usecase;

import com.ecole221.paiement.service.application.port.in.TableauBordUseCase;
import com.ecole221.paiement.service.application.port.out.TableauBordRepository;
import com.ecole221.paiement.service.application.query.DossierResume;
import com.ecole221.paiement.service.application.query.LigneImpayee;
import com.ecole221.paiement.service.application.query.ResumeTableauBord;
import com.ecole221.paiement.service.domain.valueobject.StatutDossier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TableauBordService implements TableauBordUseCase {

    private final TableauBordRepository repository;

    @Override
    @Transactional(readOnly = true)
    public ResumeTableauBord resume(String codeAnnee) {
        return repository.resume(codeAnnee);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DossierResume> dossiers(String codeAnnee, StatutDossier statut, int page, int size) {
        return repository.listerDossiers(codeAnnee, statut, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public long totalDossiers(String codeAnnee, StatutDossier statut) {
        return repository.compterDossiers(codeAnnee, statut);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LigneImpayee> impayes(String codeAnnee, int page, int size) {
        return repository.listerImpayes(codeAnnee, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public long totalImpayes(String codeAnnee) {
        return repository.compterImpayes(codeAnnee);
    }
}
