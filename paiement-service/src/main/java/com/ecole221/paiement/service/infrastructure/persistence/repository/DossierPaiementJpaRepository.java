package com.ecole221.paiement.service.infrastructure.persistence.repository;

import com.ecole221.paiement.service.domain.valueobject.StatutDossier;
import com.ecole221.paiement.service.infrastructure.persistence.entity.DossierPaiementJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DossierPaiementJpaRepository extends JpaRepository<DossierPaiementJpaEntity, UUID> {
    Optional<DossierPaiementJpaEntity> findByInscriptionId(UUID inscriptionId);
    void deleteByInscriptionId(UUID inscriptionId);

    Page<DossierPaiementJpaEntity> findByCodeAnnee(String codeAnnee, Pageable pageable);
    Page<DossierPaiementJpaEntity> findByCodeAnneeAndStatut(String codeAnnee, StatutDossier statut, Pageable pageable);

    @Query("SELECT COUNT(d) FROM DossierPaiementJpaEntity d WHERE d.codeAnnee = :codeAnnee")
    long countByCodeAnnee(@Param("codeAnnee") String codeAnnee);

    @Query("SELECT COUNT(d) FROM DossierPaiementJpaEntity d WHERE d.codeAnnee = :codeAnnee AND d.statut = :statut")
    long countByCodeAnneeAndStatut(@Param("codeAnnee") String codeAnnee, @Param("statut") StatutDossier statut);
}
