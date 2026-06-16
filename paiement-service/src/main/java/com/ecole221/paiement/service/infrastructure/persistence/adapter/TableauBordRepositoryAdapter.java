package com.ecole221.paiement.service.infrastructure.persistence.adapter;

import com.ecole221.paiement.service.application.port.out.TableauBordRepository;
import com.ecole221.paiement.service.application.query.DossierResume;
import com.ecole221.paiement.service.application.query.LigneImpayee;
import com.ecole221.paiement.service.application.query.ResumeTableauBord;
import com.ecole221.paiement.service.domain.valueobject.StatutDossier;
import com.ecole221.paiement.service.domain.valueobject.StatutLigne;
import com.ecole221.paiement.service.infrastructure.persistence.entity.DossierPaiementJpaEntity;
import com.ecole221.paiement.service.infrastructure.persistence.entity.LignePaiementJpaEntity;
import com.ecole221.paiement.service.infrastructure.persistence.repository.DossierPaiementJpaRepository;
import com.ecole221.paiement.service.infrastructure.persistence.repository.LignePaiementJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TableauBordRepositoryAdapter implements TableauBordRepository {

    private final DossierPaiementJpaRepository dossierRepo;
    private final LignePaiementJpaRepository ligneRepo;

    @Override
    public ResumeTableauBord resume(String codeAnnee) {
        long total       = dossierRepo.countByCodeAnnee(codeAnnee);
        long initialises = dossierRepo.countByCodeAnneeAndStatut(codeAnnee, StatutDossier.INITIALISE);
        long actifs      = dossierRepo.countByCodeAnneeAndStatut(codeAnnee, StatutDossier.ACTIF);
        long clotures    = dossierRepo.countByCodeAnneeAndStatut(codeAnnee, StatutDossier.CLOTURE);

        BigDecimal totalDu   = ligneRepo.sumMontantDu(codeAnnee);
        BigDecimal totalPaye = ligneRepo.sumMontantPaye(codeAnnee);
        BigDecimal restant   = totalDu.subtract(totalPaye).max(BigDecimal.ZERO);

        long lignesImpayees = ligneRepo.countByCodeAnneeAndStatutNot(codeAnnee, StatutLigne.PAYE);

        return new ResumeTableauBord(codeAnnee, total, initialises, actifs, clotures,
                totalDu, totalPaye, restant, lignesImpayees);
    }

    @Override
    public List<DossierResume> listerDossiers(String codeAnnee, StatutDossier statut, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("inscriptionId"));
        Page<DossierPaiementJpaEntity> jpaPage = (statut == null)
                ? dossierRepo.findByCodeAnnee(codeAnnee, pageable)
                : dossierRepo.findByCodeAnneeAndStatut(codeAnnee, statut, pageable);

        return jpaPage.getContent().stream().map(this::toDossierResume).toList();
    }

    @Override
    public long compterDossiers(String codeAnnee, StatutDossier statut) {
        return (statut == null)
                ? dossierRepo.countByCodeAnnee(codeAnnee)
                : dossierRepo.countByCodeAnneeAndStatut(codeAnnee, statut);
    }

    @Override
    public List<LigneImpayee> listerImpayes(String codeAnnee, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        return ligneRepo.findImpayes(codeAnnee, StatutLigne.PAYE, pageable)
                .getContent().stream().map(this::toLigneImpayee).toList();
    }

    @Override
    public long compterImpayes(String codeAnnee) {
        return ligneRepo.countByCodeAnneeAndStatutNot(codeAnnee, StatutLigne.PAYE);
    }

    private DossierResume toDossierResume(DossierPaiementJpaEntity d) {
        BigDecimal totalDu = d.getLignes().stream()
                .map(l -> l.getMontantDu())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaye = d.getLignes().stream()
                .map(l -> l.getMontantPaye())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DossierResume(
                d.getId(), d.getInscriptionId(), d.getEtudiantId(), d.getClasseId(),
                d.getCodeAnnee(), d.getStatut(),
                totalDu, totalPaye, totalDu.subtract(totalPaye).max(BigDecimal.ZERO)
        );
    }

    private LigneImpayee toLigneImpayee(LignePaiementJpaEntity l) {
        DossierPaiementJpaEntity d = l.getDossier();
        return new LigneImpayee(
                l.getId(), d.getInscriptionId(), d.getEtudiantId(), d.getClasseId(),
                d.getCodeAnnee(), l.getType(), l.getCommentaire(),
                l.getMontantDu(), l.getMontantPaye(),
                l.getMontantDu().subtract(l.getMontantPaye()).max(BigDecimal.ZERO),
                l.getStatut()
        );
    }
}
