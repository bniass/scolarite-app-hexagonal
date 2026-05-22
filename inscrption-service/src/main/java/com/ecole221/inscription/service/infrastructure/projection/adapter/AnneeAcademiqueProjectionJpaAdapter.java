package com.ecole221.inscription.service.infrastructure.projection.adapter;


import com.ecole221.inscription.service.application.port.out.repository.AnneeAcademiqueProjectionRepository;
import com.ecole221.inscription.service.domain.model.projection.AnneeAcademiqueProjection;
import com.ecole221.inscription.service.infrastructure.projection.mapper.AnneeAcademiqueProjectionJpaMapper;
import com.ecole221.inscription.service.infrastructure.projection.repository.AnneeAcademiqueProjectionJpaRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
@Service
public class AnneeAcademiqueProjectionJpaAdapter implements AnneeAcademiqueProjectionRepository {
    private final AnneeAcademiqueProjectionJpaRepository anneeAcademiqueProjectionJpaRepository;
    private final AnneeAcademiqueProjectionJpaMapper mapper;

    public AnneeAcademiqueProjectionJpaAdapter(AnneeAcademiqueProjectionJpaRepository anneeAcademiqueProjectionJpaRepository, AnneeAcademiqueProjectionJpaMapper mapper) {
        this.anneeAcademiqueProjectionJpaRepository = anneeAcademiqueProjectionJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<AnneeAcademiqueProjection> findByCodeAnnee(String codeAnnee) {
        return anneeAcademiqueProjectionJpaRepository.findById(codeAnnee)
                .map(mapper::toDomain);
    }

    @Override
    public void save(AnneeAcademiqueProjection projection) {
        anneeAcademiqueProjectionJpaRepository.save(mapper.toEntity(projection));
    }
}
