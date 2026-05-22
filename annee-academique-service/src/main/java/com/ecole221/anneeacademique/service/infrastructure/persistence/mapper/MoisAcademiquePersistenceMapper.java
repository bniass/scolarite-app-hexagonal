package com.ecole221.anneeacademique.service.infrastructure.persistence.mapper;

import com.ecole221.anneeacademique.service.domain.model.AnneeAcademique;
import com.ecole221.anneeacademique.service.domain.model.MoisAcademique;
import com.ecole221.anneeacademique.service.domain.exception.AnneeAcademiqueNotFoundException;
import com.ecole221.anneeacademique.service.infrastructure.persistence.entity.AnneeAcademiqueJpaEntity;
import com.ecole221.anneeacademique.service.infrastructure.persistence.entity.AnneeMoisJpaEntity;
import com.ecole221.anneeacademique.service.infrastructure.persistence.repository.AnneeMoisJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class MoisAcademiquePersistenceMapper {
    private final AnneeMoisJpaRepository anneeMoisJpaRepository;

    public MoisAcademiquePersistenceMapper(AnneeMoisJpaRepository anneeMoisJpaRepository) {
        this.anneeMoisJpaRepository = anneeMoisJpaRepository;
    }

    /**
     * Synchronise les mois académiques du domaine
     * vers l'entité JPA AnneeAcademique.
     *
     * Règle importante :
     * - on vide puis on reconstruit la collection
     * - permet à JPA de gérer correctement orphanRemoval
     */
    public void mapMois(
            AnneeAcademique domain,
            AnneeAcademiqueJpaEntity jpa
    ) {
        jpa.getMoisAcademiques().clear();

        for (MoisAcademique mois : domain.getMoisAcademiques()) {
            jpa.getMoisAcademiques().add(
                    new AnneeMoisJpaEntity(
                            domain.getId().getValue().getCodeAnnee(), // code année académique
                            mois.mois(),
                            mois.annee()
                    )
            );
        }
    }

    AnneeMoisJpaEntity toJpa(MoisAcademique m, String anneeCode) {
        return anneeMoisJpaRepository
                .findByAnneeAcademiqueCodeAndMoisAndAnnee(
                        anneeCode,
                        m.mois(),
                        m.annee()
                )
                .orElseThrow(() ->
                        new AnneeAcademiqueNotFoundException("Mois académique introuvable : "
                                + m.mois() + "-" + m.annee())
                );
    }

}
