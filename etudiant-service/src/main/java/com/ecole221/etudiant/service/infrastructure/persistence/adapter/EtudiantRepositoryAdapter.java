package com.ecole221.etudiant.service.infrastructure.persistence.adapter;

import com.ecole221.etudiant.service.application.port.out.EtudiantRepository;
import com.ecole221.etudiant.service.domain.model.Etudiant;
import com.ecole221.etudiant.service.infrastructure.persistence.mapper.EtudiantJpaMapper;
import com.ecole221.etudiant.service.infrastructure.persistence.repository.EtudiantJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class EtudiantRepositoryAdapter implements EtudiantRepository {

    private final EtudiantJpaRepository jpaRepository;
    private final EtudiantJpaMapper mapper;

    public EtudiantRepositoryAdapter(EtudiantJpaRepository jpaRepository, EtudiantJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Etudiant sauvegarder(Etudiant etudiant) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(etudiant)));
    }

    @Override
    public Optional<Etudiant> trouverParMatricule(String matricule) {
        return jpaRepository.findByMatricule(matricule.toUpperCase().trim()).map(mapper::toDomain);
    }

    @Override
    public Optional<Etudiant> trouverParId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existeParMatricule(String matricule) {
        return jpaRepository.existsByMatricule(matricule.toUpperCase().trim());
    }

    @Override
    public boolean existeParEmail(String email) {
        return jpaRepository.existsByEmail(email.trim().toLowerCase());
    }

    @Override
    public long compterParSuffixeAnnee(String suffixeAnnee) {
        return jpaRepository.countBySuffixeAnnee("-" + suffixeAnnee);
    }

    @Override
    public void supprimerParId(UUID id) {
        jpaRepository.deleteById(id);
    }
}
