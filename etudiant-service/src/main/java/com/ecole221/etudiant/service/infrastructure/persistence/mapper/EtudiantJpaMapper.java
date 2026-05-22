package com.ecole221.etudiant.service.infrastructure.persistence.mapper;

import com.ecole221.etudiant.service.domain.model.Etudiant;
import com.ecole221.etudiant.service.domain.valueobject.EtudiantId;
import com.ecole221.etudiant.service.domain.valueobject.Matricule;
import com.ecole221.etudiant.service.infrastructure.persistence.entity.EtudiantJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class EtudiantJpaMapper {

    public EtudiantJpaEntity toEntity(Etudiant etudiant) {
        EtudiantJpaEntity entity = new EtudiantJpaEntity();
        entity.setId(etudiant.getId().getValue());
        entity.setMatricule(etudiant.getMatricule().getValeur());
        entity.setNom(etudiant.getNom());
        entity.setPrenom(etudiant.getPrenom());
        entity.setDateNaissance(etudiant.getDateNaissance());
        return entity;
    }

    public Etudiant toDomain(EtudiantJpaEntity entity) {
        return Etudiant.reconstituer(
                new EtudiantId(entity.getId()),
                new Matricule(entity.getMatricule()),
                entity.getNom(),
                entity.getPrenom(),
                entity.getDateNaissance()
        );
    }
}
