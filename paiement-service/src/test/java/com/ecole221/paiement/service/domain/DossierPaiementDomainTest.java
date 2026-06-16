package com.ecole221.paiement.service.domain;

import com.ecole221.paiement.service.domain.exception.PaiementDomainException;
import com.ecole221.paiement.service.domain.model.*;
import com.ecole221.paiement.service.domain.valueobject.StatutDossier;
import com.ecole221.paiement.service.domain.valueobject.StatutLigne;
import com.ecole221.paiement.service.domain.valueobject.TypeLigne;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class DossierPaiementDomainTest {

    // fraisInscription=50000, autresFrais=10000, mensualite=25000
    // Minimum premier versement = 85000 | Total année = 285000
    private static final BigDecimal FRAIS        = new BigDecimal("50000");
    private static final BigDecimal MENSUALITE   = new BigDecimal("25000");
    private static final BigDecimal AUTRES_FRAIS = new BigDecimal("10000");
    private static final BigDecimal MINIMUM      = new BigDecimal("85000");
    private static final BigDecimal TOTAL        = new BigDecimal("285000");

    private DossierPaiement dossier;

    @BeforeEach
    void setUp() {
        List<MoisAcademique> mois = List.of(
                new MoisAcademique(6, 2024),   // juin   → ordre 1
                new MoisAcademique(10, 2024),  // oct    → ordre 2
                new MoisAcademique(11, 2024),  // nov    → ordre 3
                new MoisAcademique(12, 2024),  // dec    → ordre 4
                new MoisAcademique(1, 2025),   // jan    → ordre 5
                new MoisAcademique(2, 2025),   // fev    → ordre 6
                new MoisAcademique(3, 2025),   // mar    → ordre 7
                new MoisAcademique(4, 2025),   // avr    → ordre 8
                new MoisAcademique(5, 2025)    // mai    → ordre 9
        );
        dossier = DossierPaiement.initialiser(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "2024-2025", FRAIS, MENSUALITE, AUTRES_FRAIS, mois
        );
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    @Test
    void initialiser_genere_11_lignes() {
        assertThat(dossier.getLignes()).hasSize(11);
    }

    @Test
    void initialiser_contient_1_frais_inscription() {
        long count = dossier.getLignes().stream()
                .filter(l -> l.getType() == TypeLigne.FRAIS_INSCRIPTION).count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void initialiser_contient_1_autres_frais() {
        long count = dossier.getLignes().stream()
                .filter(l -> l.getType() == TypeLigne.AUTRES_FRAIS).count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void initialiser_contient_9_mensualites() {
        long count = dossier.getLignes().stream()
                .filter(l -> l.getType() == TypeLigne.MENSUALITE).count();
        assertThat(count).isEqualTo(9);
    }

    @Test
    void initialiser_statut_est_INITIALISE() {
        assertThat(dossier.getStatut()).isEqualTo(StatutDossier.INITIALISE);
    }

    @Test
    void initialiser_toutes_lignes_sont_APAYER() {
        assertThat(dossier.getLignes())
                .allMatch(l -> l.getStatut() == StatutLigne.APAYER);
    }

    @Test
    void initialiser_ne_genere_pas_devent_confirmation() {
        // L'event de confirmation n'est émis qu'au premier versement, pas à l'initialisation
        assertThat(dossier.pullDomainEvents()).isEmpty();
    }

    @Test
    void premier_versement_genere_event_DossierInitialise() {
        distribuer(MINIMUM);
        assertThat(dossier.pullDomainEvents()).hasSize(1);
    }

    // ── Distribution automatique ──────────────────────────────────────────────

    @Test
    void distribuer_minimum_exact_couvre_frais_autres_et_mois1() {
        // 50000 + 10000 + 25000 = 85000
        distribuer(MINIMUM);

        assertThat(ligneParType(TypeLigne.FRAIS_INSCRIPTION).getStatut()).isEqualTo(StatutLigne.PAYE);
        assertThat(ligneParType(TypeLigne.AUTRES_FRAIS).getStatut()).isEqualTo(StatutLigne.PAYE);
        assertThat(mensualiteParOrdre(1).getStatut()).isEqualTo(StatutLigne.PAYE);
    }

    @Test
    void distribuer_minimum_exact_passe_statut_dossier_a_ACTIF() {
        distribuer(MINIMUM);
        assertThat(dossier.getStatut()).isEqualTo(StatutDossier.ACTIF);
    }

    @Test
    void distribuer_surplus_cascade_plusieurs_mois() {
        // 50000+10000+6×25000 = 210000 → frais + autres + mois 1 à 6 payés, mois 7 non
        distribuer(new BigDecimal("210000"));

        assertThat(ligneParType(TypeLigne.FRAIS_INSCRIPTION).getStatut()).isEqualTo(StatutLigne.PAYE);
        assertThat(ligneParType(TypeLigne.AUTRES_FRAIS).getStatut()).isEqualTo(StatutLigne.PAYE);
        for (int i = 1; i <= 6; i++) {
            assertThat(mensualiteParOrdre(i).getStatut()).isEqualTo(StatutLigne.PAYE);
        }
        assertThat(mensualiteParOrdre(7).getStatut()).isEqualTo(StatutLigne.APAYER);
    }

    @Test
    void distribuer_avance_partielle_sur_mois() {
        // 85000 + 10000 = 95000 → mois1 payé, mois2 avancé (10000 versé, 15000 restant)
        distribuer(new BigDecimal("95000"));

        assertThat(mensualiteParOrdre(1).getStatut()).isEqualTo(StatutLigne.PAYE);
        assertThat(mensualiteParOrdre(2).getStatut()).isEqualTo(StatutLigne.AVANCE);
        assertThat(mensualiteParOrdre(2).getMontantRestant()).isEqualByComparingTo("15000");
    }

    @Test
    void distribuer_inferieur_au_minimum_leve_exception() {
        // 80000 < 85000 minimum
        assertThatThrownBy(() -> distribuer(new BigDecimal("80000")))
                .isInstanceOf(PaiementDomainException.class)
                .hasMessageContaining("minimum");
    }

    @Test
    void distribuer_depasse_total_restant_leve_exception() {
        // 300000 > 285000 total
        assertThatThrownBy(() -> distribuer(new BigDecimal("300000")))
                .isInstanceOf(PaiementDomainException.class)
                .hasMessageContaining("dépasse");
    }

    @Test
    void distribuer_montant_nul_leve_exception() {
        assertThatThrownBy(() -> distribuer(BigDecimal.ZERO))
                .isInstanceOf(PaiementDomainException.class);
    }

    @Test
    void distribuer_apres_premier_versement_pas_de_minimum() {
        distribuer(MINIMUM);
        // Deuxième versement peut être inférieur au minimum
        assertThatCode(() -> distribuer(new BigDecimal("5000")))
                .doesNotThrowAnyException();
    }

    @Test
    void distribuer_plafond_respecte_apres_premier_versement() {
        // Reste = 285000 - 85000 = 200000, verser 201000 → interdit
        distribuer(MINIMUM);
        assertThatThrownBy(() -> distribuer(new BigDecimal("201000")))
                .isInstanceOf(PaiementDomainException.class)
                .hasMessageContaining("dépasse");
    }

    @Test
    void distribuer_cloture_le_dossier_si_tout_paye() {
        distribuer(TOTAL);
        assertThat(dossier.getStatut()).isEqualTo(StatutDossier.CLOTURE);
    }

    @Test
    void distribuer_sur_dossier_cloture_leve_exception() {
        distribuer(TOTAL);
        assertThatThrownBy(() -> distribuer(new BigDecimal("1")))
                .isInstanceOf(PaiementDomainException.class)
                .hasMessageContaining("clôturé");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void distribuer(BigDecimal montant) {
        dossier.distribuerVersement(montant, LocalDate.now(), MoyenPaiement.comptant());
    }

    private LignePaiement ligneParType(TypeLigne type) {
        return dossier.getLignes().stream()
                .filter(l -> l.getType() == type)
                .findFirst().orElseThrow();
    }

    private LignePaiement mensualiteParOrdre(int ordre) {
        return dossier.getLignes().stream()
                .filter(l -> l.getType() == TypeLigne.MENSUALITE && l.getOrdreReglement() == ordre)
                .findFirst().orElseThrow();
    }
}
