package com.ecole221.paiement.service.infrastructure.persistence.adapter;

import com.ecole221.paiement.service.application.port.out.DossierPaiementRepository;
import com.ecole221.paiement.service.domain.model.DossierPaiement;
import com.ecole221.paiement.service.infrastructure.persistence.mapper.DossierPaiementMapper;
import com.ecole221.paiement.service.infrastructure.persistence.repository.DossierPaiementJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DossierPaiementRepositoryAdapter implements DossierPaiementRepository {

    private final DossierPaiementJpaRepository jpaRepository;
    private final DossierPaiementMapper mapper;

    @Override
    public DossierPaiement sauvegarder(DossierPaiement dossier) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(dossier)));
    }

    @Override
    public Optional<DossierPaiement> trouverParInscriptionId(UUID inscriptionId) {
        return jpaRepository.findByInscriptionId(inscriptionId).map(mapper::toDomain);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void supprimerParInscriptionId(UUID inscriptionId) {
        jpaRepository.deleteByInscriptionId(inscriptionId);
    }
}
