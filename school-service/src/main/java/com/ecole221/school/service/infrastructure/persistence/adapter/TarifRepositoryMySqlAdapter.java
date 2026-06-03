package com.ecole221.school.service.infrastructure.persistence.adapter;

import com.ecole221.school.service.application.port.out.repository.TarifRepository;
import com.ecole221.school.service.domain.model.Tarif;
import com.ecole221.school.service.domain.model.TarifId;
import com.ecole221.school.service.infrastructure.persistence.entity.TarifJpaEntity;
import com.ecole221.school.service.infrastructure.persistence.mapper.TarifPersistenceMapper;
import com.ecole221.school.service.infrastructure.persistence.repository.TarifJpaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Profile("mysql")
public class TarifRepositoryMySqlAdapter implements TarifRepository {

    private final TarifJpaRepository jpaRepository;
    private final TarifPersistenceMapper mapper;

    public TarifRepositoryMySqlAdapter(TarifJpaRepository jpaRepository, TarifPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public void save(Tarif tarif) {
        TarifJpaEntity entity = jpaRepository.findById(tarif.getId().getValue())
                .orElseGet(() -> mapper.toJpa(tarif));
        jpaRepository.save(entity);
    }

    @Override
    public Optional<Tarif> findById(TarifId id) {
        return jpaRepository.findById(id.getValue()).map(mapper::toDomain);
    }
}
