package com.ecole221.paiement.service.infrastructure.persistence.mapper;

import com.ecole221.paiement.service.domain.model.*;
import com.ecole221.paiement.service.infrastructure.persistence.entity.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DossierPaiementMapper {

    public DossierPaiementJpaEntity toEntity(DossierPaiement domain) {
        DossierPaiementJpaEntity entity = new DossierPaiementJpaEntity();
        entity.setId(domain.getId());
        entity.setInscriptionId(domain.getInscriptionId());
        entity.setEtudiantId(domain.getEtudiantId());
        entity.setClasseId(domain.getClasseId());
        entity.setCodeAnnee(domain.getCodeAnnee());
        entity.setFraisInscription(domain.getFraisInscription());
        entity.setMensualite(domain.getMensualite());
        entity.setAutresFrais(domain.getAutresFrais());
        entity.setStatut(domain.getStatut());

        List<LignePaiementJpaEntity> ligneEntities = new ArrayList<>();
        for (LignePaiement ligne : domain.getLignes()) {
            ligneEntities.add(toLigneEntity(ligne, entity));
        }
        entity.setLignes(ligneEntities);
        return entity;
    }

    private LignePaiementJpaEntity toLigneEntity(LignePaiement ligne, DossierPaiementJpaEntity dossier) {
        LignePaiementJpaEntity e = new LignePaiementJpaEntity();
        e.setId(ligne.getId());
        e.setDossier(dossier);
        e.setType(ligne.getType());
        if (ligne.getMoisAcademique() != null) {
            e.setMoisAcademiqueMois(ligne.getMoisAcademique().mois());
            e.setMoisAcademiqueAnnee(ligne.getMoisAcademique().annee());
        }
        e.setOrdreReglement(ligne.getOrdreReglement());
        e.setMontantDu(ligne.getMontantDu());
        e.setMontantPaye(ligne.getMontantPaye());
        e.setCommentaire(ligne.getCommentaire());
        e.setStatut(ligne.getStatut());

        List<VersementJpaEntity> versementEntities = new ArrayList<>();
        for (Versement v : ligne.getVersements()) {
            versementEntities.add(toVersementEntity(v, e));
        }
        e.setVersements(versementEntities);
        return e;
    }

    private VersementJpaEntity toVersementEntity(Versement v, LignePaiementJpaEntity ligne) {
        VersementJpaEntity e = new VersementJpaEntity();
        e.setId(v.getId());
        e.setLigne(ligne);
        e.setMontant(v.getMontant());
        e.setDatePaiement(v.getDatePaiement());
        e.setMoyen(toMoyenEntity(v.getMoyen()));
        return e;
    }

    private MoyenPaiementJpaEntity toMoyenEntity(MoyenPaiement m) {
        MoyenPaiementJpaEntity e = new MoyenPaiementJpaEntity();
        e.setId(m.getId());
        e.setType(m.getType());
        e.setOperateur(m.getOperateur());
        e.setReferencePaiement(m.getReferencePaiement());
        e.setNomBanque(m.getNomBanque());
        e.setNumeroTransaction(m.getNumeroTransaction());
        return e;
    }

    public DossierPaiement toDomain(DossierPaiementJpaEntity entity) {
        List<LignePaiement> lignes = entity.getLignes().stream()
                .map(this::toLigneDomain)
                .toList();

        return DossierPaiement.reconstituer(
                entity.getId(),
                entity.getInscriptionId(),
                entity.getEtudiantId(),
                entity.getClasseId(),
                entity.getCodeAnnee(),
                entity.getFraisInscription(),
                entity.getMensualite(),
                entity.getAutresFrais(),
                entity.getStatut(),
                lignes
        );
    }

    private LignePaiement toLigneDomain(LignePaiementJpaEntity e) {
        MoisAcademique mois = (e.getMoisAcademiqueMois() != null)
                ? new MoisAcademique(e.getMoisAcademiqueMois(), e.getMoisAcademiqueAnnee())
                : null;

        List<Versement> versements = e.getVersements().stream()
                .map(this::toVersementDomain)
                .toList();

        return LignePaiement.reconstituer(
                e.getId(), e.getType(), mois, e.getOrdreReglement(),
                e.getMontantDu(), e.getMontantPaye(), e.getCommentaire(),
                e.getStatut(), versements
        );
    }

    private Versement toVersementDomain(VersementJpaEntity e) {
        MoyenPaiementJpaEntity m = e.getMoyen();
        MoyenPaiement moyen = MoyenPaiement.reconstituer(
                m.getId(), m.getType(), m.getOperateur(),
                m.getReferencePaiement(), m.getNomBanque(), m.getNumeroTransaction());
        return Versement.reconstituer(e.getId(), e.getMontant(), e.getDatePaiement(), moyen);
    }
}
