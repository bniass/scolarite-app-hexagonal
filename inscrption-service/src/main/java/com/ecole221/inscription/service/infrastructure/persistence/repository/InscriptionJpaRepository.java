package com.ecole221.inscription.service.infrastructure.persistence.repository;

import com.ecole221.inscription.service.infrastructure.persistence.entity.InscriptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InscriptionJpaRepository extends JpaRepository<InscriptionJpaEntity, UUID> {}
