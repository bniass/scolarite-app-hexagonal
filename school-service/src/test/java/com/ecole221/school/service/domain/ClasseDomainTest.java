package com.ecole221.school.service.domain;

import com.ecole221.school.service.domain.exception.SchoolException;
import com.ecole221.school.service.domain.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class ClasseDomainTest {

    private static final FiliereId FILIERE_ID = FiliereId.of(UUID.randomUUID());

    private Classe classeValide() {
        return Classe.creer("L1-INFO", "L1 Informatique", Cycle.LICENCE, Niveau.L1, FILIERE_ID);
    }

    @Test
    void creer_classe_valide() {
        Classe classe = classeValide();

        assertThat(classe.getCode()).isEqualTo("L1-INFO");
        assertThat(classe.getNom()).isEqualTo("L1 Informatique");
        assertThat(classe.getCycle()).isEqualTo(Cycle.LICENCE);
        assertThat(classe.getNiveau()).isEqualTo(Niveau.L1);
        assertThat(classe.getId()).isNotNull();
    }

    @Test
    void creer_normalise_le_code_en_majuscules() {
        Classe classe = Classe.creer("l1-info", "L1 Info", Cycle.LICENCE, Niveau.L1, FILIERE_ID);
        assertThat(classe.getCode()).isEqualTo("L1-INFO");
    }

    @Test
    void creer_genere_un_event_ClasseCreee() {
        Classe classe = classeValide();
        assertThat(classe.pullDomainEvents()).hasSize(1);
    }

    @Test
    void creer_niveau_master_sur_cycle_licence_leve_exception() {
        assertThatThrownBy(() -> Classe.creer("M1-INFO", "M1 Info", Cycle.LICENCE, Niveau.M1, FILIERE_ID))
                .isInstanceOf(SchoolException.class)
                .hasMessageContaining("Master");
    }

    @Test
    void creer_niveau_licence_sur_cycle_master_leve_exception() {
        assertThatThrownBy(() -> Classe.creer("L1-INFO", "L1 Info", Cycle.MASTER, Niveau.L1, FILIERE_ID))
                .isInstanceOf(SchoolException.class)
                .hasMessageContaining("Licence");
    }

    @Test
    void creer_code_null_leve_exception() {
        assertThatThrownBy(() -> Classe.creer(null, "Nom", Cycle.LICENCE, Niveau.L1, FILIERE_ID))
                .isInstanceOf(SchoolException.class);
    }

    @Test
    void affecter_tarif_ajoute_dans_historique() {
        Classe classe = classeValide();
        TarifId tarifId = TarifId.of(UUID.randomUUID());

        classe.affecterTarif(tarifId);

        assertThat(classe.getTarifActif()).isNotNull();
        assertThat(classe.getTarifActif().getTarifId()).isEqualTo(tarifId);
    }

    @Test
    void affecter_tarif_genere_event_TarifAffecte() {
        Classe classe = classeValide();
        classe.pullDomainEvents(); // vider l'event de création

        classe.affecterTarif(TarifId.of(UUID.randomUUID()));

        assertThat(classe.pullDomainEvents()).hasSize(1);
    }

    @Test
    void affecter_nouveau_tarif_desactive_lancien() {
        Classe classe = classeValide();
        TarifId tarif1 = TarifId.of(UUID.randomUUID());
        TarifId tarif2 = TarifId.of(UUID.randomUUID());

        classe.affecterTarif(tarif1);
        classe.affecterTarif(tarif2);

        assertThat(classe.getTarifActif().getTarifId()).isEqualTo(tarif2);
        long nbActifs = classe.getHistoriqueTarifs().stream().filter(ClasseTarif::isActif).count();
        assertThat(nbActifs).isEqualTo(1);
    }

    @Test
    void get_tarif_actif_sans_tarif_retourne_null() {
        Classe classe = classeValide();
        assertThat(classe.getTarifActif()).isNull();
    }

    @Test
    void affecter_tarif_null_leve_exception() {
        Classe classe = classeValide();
        assertThatThrownBy(() -> classe.affecterTarif(null))
                .isInstanceOf(NullPointerException.class);
    }
}
