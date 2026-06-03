package com.ecole221.anneeacademique.service.infrastructure.persistence.mapper;

import com.ecole221.anneeacademique.service.domain.model.AnneeAcademique;
import com.ecole221.anneeacademique.service.domain.model.DatesAnnee;
import com.ecole221.anneeacademique.service.domain.model.MoisAcademique;
import com.ecole221.anneeacademique.service.domain.model.Statut;
import com.ecole221.anneeacademique.service.domain.state.AnneeBrouillon;
import com.ecole221.anneeacademique.service.infrastructure.persistence.entity.AnneeAcademiqueJpaEntity;
import com.ecole221.anneeacademique.service.infrastructure.persistence.entity.AnneeMoisJpaEntity;
import org.springframework.stereotype.Component;

import java.util.List;

    @Component
    public class AnneeAcademiquePersistenceMapper {

        /* =======================
           DOMAIN → JPA
           ======================= */

        public AnneeAcademiqueJpaEntity toJpa(AnneeAcademique domain) {
            AnneeAcademiqueJpaEntity entity = new AnneeAcademiqueJpaEntity();
            entity.setCode(domain.getId().getValue().getCodeAnnee());
            entity.setDateDebut(domain.getDateDebut());
            entity.setDateFin(domain.getDateFin());
            entity.setDateDebutInscriptions(domain.getDateOuvertureInscription());
            entity.setDateFinInscriptions(domain.getDateFinInscription());
            entity.setDatePublication(domain.getDatePublication());
            entity.setStatut(
                    EtatAnneeFactory.toStatut(domain.getEtatAnnee())
            );

            /* =======================
               MOIS ACADEMIQUES
               ======================= */
            if(domain.getEtatAnnee() instanceof AnneeBrouillon){
                entity.getMoisAcademiques().clear();

                for (MoisAcademique m : domain.getMoisAcademiques()) {
                    entity.getMoisAcademiques().add(
                            new AnneeMoisJpaEntity(entity, m.mois(), m.annee())
                    );
                }
            }

            return entity;
        }

            /* =======================
           DOMAIN → JPA (UPDATE)
           ======================= */

        public void updateJpa(
                AnneeAcademique domain,
                AnneeAcademiqueJpaEntity entity
        ) {
            entity.setDateDebut(domain.getDateDebut());
            entity.setDateFin(domain.getDateFin());
            entity.setDateDebutInscriptions(domain.getDateOuvertureInscription());
            entity.setDateFinInscriptions(domain.getDateFinInscription());
            entity.setDatePublication(domain.getDatePublication());
            entity.setStatut(
                    EtatAnneeFactory.toStatut(domain.getEtatAnnee())
            );

            if (domain.getEtatAnnee() instanceof AnneeBrouillon) {
                entity.getMoisAcademiques().clear();
                for (MoisAcademique m : domain.getMoisAcademiques()) {
                    entity.getMoisAcademiques().add(
                            new AnneeMoisJpaEntity(entity, m.mois(), m.annee())
                    );
                }
            }
        }


        /* =======================
           JPA → DOMAIN
           ======================= */

        public AnneeAcademique toDomain(AnneeAcademiqueJpaEntity entity) {

            List<MoisAcademique> mois = entity.getMoisAcademiques()
                    .stream()
                    .map(m -> new MoisAcademique(
                            m.getMois(),
                            m.getAnnee()
                    ))
                    .toList();

            return AnneeAcademique.reconstituer(
                    entity.getCode(),
                    new DatesAnnee(
                            entity.getDateDebut(),
                            entity.getDateFin(),
                            entity.getDateDebutInscriptions(),
                            entity.getDateFinInscriptions()
                    ),
                    entity.getDatePublication(),
                    EtatAnneeFactory.fromStatut(
                            Statut.valueOf(entity.getStatut().name())
                    ),
                    mois
            );
        }
    }
