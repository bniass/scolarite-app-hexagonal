package com.ecole221.paiement.service.infrastructure.persistence.repository;

import com.ecole221.paiement.service.infrastructure.persistence.entity.LignePaiementJpaEntity;
import com.ecole221.paiement.service.domain.valueobject.StatutLigne;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.UUID;

public interface LignePaiementJpaRepository extends JpaRepository<LignePaiementJpaEntity, UUID> {

    @Query("SELECT COALESCE(SUM(l.montantDu), 0) FROM LignePaiementJpaEntity l WHERE l.dossier.codeAnnee = :codeAnnee")
    BigDecimal sumMontantDu(@Param("codeAnnee") String codeAnnee);

    @Query("SELECT COALESCE(SUM(l.montantPaye), 0) FROM LignePaiementJpaEntity l WHERE l.dossier.codeAnnee = :codeAnnee")
    BigDecimal sumMontantPaye(@Param("codeAnnee") String codeAnnee);

    @Query("SELECT COUNT(l) FROM LignePaiementJpaEntity l WHERE l.dossier.codeAnnee = :codeAnnee AND l.statut <> :statut")
    long countByCodeAnneeAndStatutNot(@Param("codeAnnee") String codeAnnee, @Param("statut") StatutLigne statut);

    @Query("SELECT l FROM LignePaiementJpaEntity l WHERE l.dossier.codeAnnee = :codeAnnee AND l.statut <> :statut ORDER BY l.dossier.inscriptionId, l.ordreReglement")
    Page<LignePaiementJpaEntity> findImpayes(@Param("codeAnnee") String codeAnnee, @Param("statut") StatutLigne statut, Pageable pageable);
}
