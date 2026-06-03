package com.ecole221.school.service.domain;

import com.ecole221.school.service.domain.exception.SchoolException;
import com.ecole221.school.service.domain.model.Filiere;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class FiliereDomainTest {

    @Test
    void creer_filiere_avec_donnees_valides() {
        Filiere filiere = Filiere.creer("INFO", "Informatique");

        assertThat(filiere.getCode()).isEqualTo("INFO");
        assertThat(filiere.getNom()).isEqualTo("Informatique");
        assertThat(filiere.getId()).isNotNull();
    }

    @Test
    void creer_normalise_le_code_en_majuscules() {
        Filiere filiere = Filiere.creer("info", "Informatique");
        assertThat(filiere.getCode()).isEqualTo("INFO");
    }

    @Test
    void creer_genere_un_event_FiliereCreee() {
        Filiere filiere = Filiere.creer("GES", "Gestion");
        assertThat(filiere.pullDomainEvents()).hasSize(1);
    }

    @Test
    void creer_code_null_leve_exception() {
        assertThatThrownBy(() -> Filiere.creer(null, "Nom"))
                .isInstanceOf(SchoolException.class)
                .hasMessageContaining("code");
    }

    @Test
    void creer_code_vide_leve_exception() {
        assertThatThrownBy(() -> Filiere.creer("  ", "Nom"))
                .isInstanceOf(SchoolException.class);
    }

    @Test
    void creer_nom_null_leve_exception() {
        assertThatThrownBy(() -> Filiere.creer("INFO", null))
                .isInstanceOf(SchoolException.class)
                .hasMessageContaining("nom");
    }
}
