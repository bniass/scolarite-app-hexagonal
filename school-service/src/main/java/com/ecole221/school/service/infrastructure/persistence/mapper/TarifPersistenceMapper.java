package com.ecole221.school.service.infrastructure.persistence.mapper;

import com.ecole221.school.service.domain.model.Tarif;
import com.ecole221.school.service.domain.model.TarifId;
import com.ecole221.school.service.infrastructure.persistence.entity.TarifJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class TarifPersistenceMapper {

    public TarifJpaEntity toJpa(Tarif domain) {
        TarifJpaEntity entity = new TarifJpaEntity();
        entity.setId(domain.getId().getValue());
        entity.setFraisInscription(domain.getFraisInscription());
        entity.setMensualite(domain.getMensualite());
        entity.setAutresFrais(domain.getAutresFrais());
        return entity;
    }

    public Tarif toDomain(TarifJpaEntity entity) {
        return Tarif.reconstituer(
                TarifId.of(entity.getId()),
                entity.getFraisInscription(),
                entity.getMensualite(),
                entity.getAutresFrais()
        );
    }
}
