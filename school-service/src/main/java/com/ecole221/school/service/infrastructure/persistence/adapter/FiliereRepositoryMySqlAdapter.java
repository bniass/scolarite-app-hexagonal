package com.ecole221.school.service.infrastructure.persistence.adapter;

import com.ecole221.school.service.application.port.out.repository.FiliereRepository;
import com.ecole221.school.service.domain.model.Filiere;
import com.ecole221.school.service.domain.model.FiliereId;
import com.ecole221.school.service.infrastructure.persistence.entity.FiliereJpaEntity;
import com.ecole221.school.service.infrastructure.persistence.mapper.FilierePersistenceMapper;
import com.ecole221.school.service.infrastructure.persistence.repository.FiliereJpaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Profile("mysql")
public class FiliereRepositoryMySqlAdapter implements FiliereRepository {

    private final FiliereJpaRepository jpaRepository;
    private final FilierePersistenceMapper mapper;

    public FiliereRepositoryMySqlAdapter(FiliereJpaRepository jpaRepository, FilierePersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public void save(Filiere filiere) {
        FiliereJpaEntity entity = jpaRepository.findById(filiere.getId().getValue())
                .orElseGet(() -> mapper.toJpa(filiere));

        if (entity.getId() != null) {
            entity.setCode(filiere.getCode());
            entity.setNom(filiere.getNom());
        }

        jpaRepository.save(entity);
    }

    @Override
    public Optional<Filiere> findById(FiliereId id) {
        return jpaRepository.findById(id.getValue()).map(mapper::toDomain);
    }

    @Override
    public Optional<Filiere> findByCode(String code) {
        return jpaRepository.findByCode(code).map(mapper::toDomain);
    }
}
