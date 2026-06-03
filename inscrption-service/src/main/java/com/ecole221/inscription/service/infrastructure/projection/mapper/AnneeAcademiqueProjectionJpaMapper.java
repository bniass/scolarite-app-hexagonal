package com.ecole221.inscription.service.infrastructure.projection.mapper;

import com.ecole221.inscription.service.domain.model.projection.AnneeAcademiqueProjection;
import com.ecole221.inscription.service.domain.valueobject.CodeAnnee;
import com.ecole221.inscription.service.domain.valueobject.EtatAnnee;
import com.ecole221.inscription.service.infrastructure.projection.entity.AnneeAcademiqueProjectionJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class AnneeAcademiqueProjectionJpaMapper {

    public AnneeAcademiqueProjection toDomain(AnneeAcademiqueProjectionJpaEntity entity) {
        return new AnneeAcademiqueProjection(
                new CodeAnnee(entity.getCodeAnnee()),
                EtatAnnee.valueOf(entity.getEtatAnnee()),
                entity.getMoisAcademiquesJson()
        );
    }

    public AnneeAcademiqueProjectionJpaEntity toEntity(AnneeAcademiqueProjection domain) {
        return new AnneeAcademiqueProjectionJpaEntity(
                domain.getCodeAnnee().getValue(),
                domain.getEtatAnnee().name(),
                domain.getMoisAcademiquesJson()
        );
    }
}
