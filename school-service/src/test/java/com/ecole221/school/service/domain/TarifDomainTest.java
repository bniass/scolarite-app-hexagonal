package com.ecole221.school.service.domain;

import com.ecole221.school.service.domain.exception.SchoolException;
import com.ecole221.school.service.domain.model.Tarif;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class TarifDomainTest {

    @Test
    void creer_tarif_valide() {
        Tarif tarif = Tarif.creer(
                new BigDecimal("50000"),
                new BigDecimal("25000"),
                new BigDecimal("10000")
        );

        assertThat(tarif.getFraisInscription()).isEqualByComparingTo("50000");
        assertThat(tarif.getMensualite()).isEqualByComparingTo("25000");
        assertThat(tarif.getAutresFrais()).isEqualByComparingTo("10000");
        assertThat(tarif.getId()).isNotNull();
    }

    @Test
    void creer_genere_un_event_TarifCree() {
        Tarif tarif = Tarif.creer(new BigDecimal("50000"), new BigDecimal("25000"), new BigDecimal("10000"));
        assertThat(tarif.pullDomainEvents()).hasSize(1);
    }

    @Test
    void creer_frais_inscription_negatif_leve_exception() {
        assertThatThrownBy(() -> Tarif.creer(new BigDecimal("-1"), BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(SchoolException.class)
                .hasMessageContaining("frais d'inscription");
    }

    @Test
    void creer_mensualite_negative_leve_exception() {
        assertThatThrownBy(() -> Tarif.creer(BigDecimal.ZERO, new BigDecimal("-1"), BigDecimal.ZERO))
                .isInstanceOf(SchoolException.class)
                .hasMessageContaining("mensualité");
    }

    @Test
    void creer_autres_frais_negatifs_leve_exception() {
        assertThatThrownBy(() -> Tarif.creer(BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("-1")))
                .isInstanceOf(SchoolException.class)
                .hasMessageContaining("autres frais");
    }

    @Test
    void creer_avec_valeurs_a_zero_est_accepte() {
        assertThatCode(() -> Tarif.creer(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
                .doesNotThrowAnyException();
    }
}
