package com.ecole221.school.service.infrastructure.persistence.mapper;

import com.ecole221.school.service.domain.model.*;
import com.ecole221.school.service.infrastructure.persistence.entity.ClasseJpaEntity;
import com.ecole221.school.service.infrastructure.persistence.entity.ClasseTarifJpaEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClassePersistenceMapper {

    public ClasseJpaEntity toJpa(Classe domain) {
        ClasseJpaEntity entity = new ClasseJpaEntity();
        entity.setId(domain.getId().getValue());
        entity.setCode(domain.getCode());
        entity.setNom(domain.getNom());
        entity.setCycle(domain.getCycle());
        entity.setNiveau(domain.getNiveau());
        entity.setFiliereId(domain.getFiliereId().getValue());
        return entity;
    }

    public void syncHistoriqueTarifs(Classe domain, ClasseJpaEntity entity) {
        entity.getHistoriqueTarifs().clear();
        for (ClasseTarif ct : domain.getHistoriqueTarifs()) {
            ClasseTarifJpaEntity ctEntity = new ClasseTarifJpaEntity(
                    entity,
                    ct.getTarifId().getValue(),
                    ct.getDateActivation(),
                    ct.isActif()
            );
            ctEntity.setDateDesactivation(ct.getDateDesactivation());
            entity.getHistoriqueTarifs().add(ctEntity);
        }
    }

    public Classe toDomain(ClasseJpaEntity entity) {
        List<ClasseTarif> historique = entity.getHistoriqueTarifs().stream()
                .map(ct -> ClasseTarif.reconstituer(
                        TarifId.of(ct.getTarifId()),
                        ct.getDateActivation(),
                        ct.getDateDesactivation(),
                        ct.isActif()
                ))
                .toList();

        return Classe.reconstituer(
                ClasseId.of(entity.getId()),
                entity.getCode(),
                entity.getNom(),
                entity.getCycle(),
                entity.getNiveau(),
                FiliereId.of(entity.getFiliereId()),
                historique
        );
    }
}
