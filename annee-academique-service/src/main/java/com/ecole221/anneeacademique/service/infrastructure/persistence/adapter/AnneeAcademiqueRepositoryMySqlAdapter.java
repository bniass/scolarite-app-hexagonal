package com.ecole221.anneeacademique.service.infrastructure.persistence.adapter;

import com.ecole221.anneeacademique.service.application.port.out.repository.AnneeAcademiqueRepository;
import com.ecole221.anneeacademique.service.domain.model.AnneeAcademique;
import com.ecole221.anneeacademique.service.domain.model.Statut;
import com.ecole221.anneeacademique.service.infrastructure.persistence.entity.AnneeAcademiqueJpaEntity;
import com.ecole221.anneeacademique.service.infrastructure.persistence.mapper.AnneeAcademiquePersistenceMapper;
import com.ecole221.anneeacademique.service.infrastructure.persistence.repository.AnneeAcademiqueJpaRepository;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Component
@Profile("mysql")
public class AnneeAcademiqueRepositoryMySqlAdapter
        implements AnneeAcademiqueRepository {

    private final AnneeAcademiqueJpaRepository jpaRepository;
    private final AnneeAcademiquePersistenceMapper mapper;

    @Override
    public Optional<AnneeAcademique> findByCode(String code) {
        return jpaRepository.findByCode(code)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByStatutNot(Statut statut) {
        return jpaRepository.existsByStatutNot(statut);
    }

    @Override
    public List<AnneeAcademique> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void save(AnneeAcademique annee) {
        AnneeAcademiqueJpaEntity entity =
                jpaRepository.findByCode(annee.getId().getValue().getCodeAnnee())
                        .orElseGet(() ->
                                mapper.toJpa(annee) // CREATE
                        );

        if (entity.getCode() != null) {
            mapper.updateJpa(annee, entity); // UPDATE
        }

        jpaRepository.save(entity);
    }
}

