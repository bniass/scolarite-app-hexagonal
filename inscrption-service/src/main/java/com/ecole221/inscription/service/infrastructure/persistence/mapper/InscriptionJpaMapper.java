package com.ecole221.inscription.service.infrastructure.persistence.mapper;

import com.ecole221.inscription.service.domain.model.Inscription;
import com.ecole221.inscription.service.infrastructure.persistence.entity.InscriptionJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class InscriptionJpaMapper {

    public InscriptionJpaEntity toEntity(Inscription domain) {
        InscriptionJpaEntity e = new InscriptionJpaEntity();
        e.setId(domain.getId());
        e.setEtudiantId(domain.getEtudiantId());
        e.setClasseId(domain.getClasseId());
        e.setCodeAnnee(domain.getCodeAnnee());
        e.setFraisInscription(domain.getFraisInscription());
        e.setMensualite(domain.getMensualite());
        e.setAutresFrais(domain.getAutresFrais());
        e.setMoisAcademiquesJson(domain.getMoisAcademiquesJson());
        e.setStatut(domain.getStatut());
        e.setEtudiantNouveau(domain.isEtudiantNouveau());
        e.setCreeLe(domain.getCreeLe());
        return e;
    }

    public Inscription toDomain(InscriptionJpaEntity e) {
        return Inscription.reconstituer(
                e.getId(), e.getEtudiantId(), e.isEtudiantNouveau(), e.getClasseId(), e.getCodeAnnee(),
                e.getFraisInscription(), e.getMensualite(), e.getAutresFrais(),
                e.getMoisAcademiquesJson(), e.getStatut(), e.getCreeLe()
        );
    }
}
