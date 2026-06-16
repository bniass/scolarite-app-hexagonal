package com.ecole221.paiement.service.infrastructure.persistence.repository;

import com.ecole221.paiement.service.infrastructure.persistence.entity.DossierPaiementJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DossierPaiementJpaRepository extends JpaRepository<DossierPaiementJpaEntity, UUID> {
    Optional<DossierPaiementJpaEntity> findByInscriptionId(UUID inscriptionId);
    void deleteByInscriptionId(UUID inscriptionId);
}
