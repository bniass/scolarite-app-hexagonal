package com.ecole221.school.service.infrastructure.persistence.adapter;

import com.ecole221.school.service.application.port.out.repository.ClasseRepository;
import com.ecole221.school.service.domain.model.Classe;
import com.ecole221.school.service.domain.model.ClasseId;
import com.ecole221.school.service.infrastructure.persistence.entity.ClasseJpaEntity;
import com.ecole221.school.service.infrastructure.persistence.mapper.ClassePersistenceMapper;
import com.ecole221.school.service.infrastructure.persistence.repository.ClasseJpaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Profile("mysql")
public class ClasseRepositoryMySqlAdapter implements ClasseRepository {

    private final ClasseJpaRepository jpaRepository;
    private final ClassePersistenceMapper mapper;

    public ClasseRepositoryMySqlAdapter(ClasseJpaRepository jpaRepository, ClassePersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public void save(Classe classe) {
        ClasseJpaEntity entity = jpaRepository.findById(classe.getId().getValue())
                .orElseGet(() -> mapper.toJpa(classe));

        if (entity.getId() != null) {
            entity.setCode(classe.getCode());
            entity.setNom(classe.getNom());
            entity.setCycle(classe.getCycle());
            entity.setNiveau(classe.getNiveau());
            entity.setFiliereId(classe.getFiliereId().getValue());
        }

        mapper.syncHistoriqueTarifs(classe, entity);
        jpaRepository.save(entity);
    }

    @Override
    public Optional<Classe> findById(ClasseId id) {
        return jpaRepository.findById(id.getValue()).map(mapper::toDomain);
    }

    @Override
    public Optional<Classe> findByCode(String code) {
        return jpaRepository.findByCode(code).map(mapper::toDomain);
    }
}
