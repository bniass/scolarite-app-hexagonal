package com.ecole221.inscription.service.infrastructure.persistence.adapter;

import com.ecole221.inscription.service.application.port.out.InscriptionRepository;
import com.ecole221.inscription.service.domain.model.Inscription;
import com.ecole221.inscription.service.infrastructure.persistence.mapper.InscriptionJpaMapper;
import com.ecole221.inscription.service.infrastructure.persistence.repository.InscriptionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InscriptionRepositoryAdapter implements InscriptionRepository {

    private final InscriptionJpaRepository jpaRepository;
    private final InscriptionJpaMapper mapper;

    @Override
    public Inscription sauvegarder(Inscription inscription) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(inscription)));
    }

    @Override
    public Optional<Inscription> trouverParId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public void supprimer(UUID id) {
        jpaRepository.deleteById(id);
    }
}
