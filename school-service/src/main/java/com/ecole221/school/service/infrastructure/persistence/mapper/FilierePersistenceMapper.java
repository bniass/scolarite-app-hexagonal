package com.ecole221.school.service.infrastructure.persistence.mapper;

import com.ecole221.school.service.domain.model.Filiere;
import com.ecole221.school.service.domain.model.FiliereId;
import com.ecole221.school.service.infrastructure.persistence.entity.FiliereJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class FilierePersistenceMapper {

    public FiliereJpaEntity toJpa(Filiere domain) {
        FiliereJpaEntity entity = new FiliereJpaEntity();
        entity.setId(domain.getId().getValue());
        entity.setCode(domain.getCode());
        entity.setNom(domain.getNom());
        return entity;
    }

    public Filiere toDomain(FiliereJpaEntity entity) {
        return Filiere.reconstituer(
                FiliereId.of(entity.getId()),
                entity.getCode(),
                entity.getNom()
        );
    }
}
