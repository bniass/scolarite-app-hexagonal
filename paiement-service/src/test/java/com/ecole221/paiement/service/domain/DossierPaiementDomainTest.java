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

    private DossierPaiement dossier;

    @BeforeEach
    void setUp() {
        List<MoisAcademique> mois = List.of(
                new MoisAcademique(9, 2024),
                new MoisAcademique(10, 2024),
                new MoisAcademique(11, 2024),
                new MoisAcademique(12, 2024),
                new MoisAcademique(1, 2025),
                new MoisAcademique(2, 2025),
                new MoisAcademique(3, 2025),
                new MoisAcademique(4, 2025),
                new MoisAcademique(5, 2025)
        );
        dossier = DossierPaiement.initialiser(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "2024-2025",
                new BigDecimal("50000"),
                new BigDecimal("25000"),
                new BigDecimal("10000"),
                mois
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
    void initialiser_genere_un_event_DossierInitialise() {
        assertThat(dossier.pullDomainEvents()).hasSize(1);
    }

    // ── Versement sur FRAIS_INSCRIPTION ───────────────────────────────────────

    @Test
    void versement_frais_inscription_exact_marque_ligne_PAYE() {
        LignePaiement ligne = ligneParType(TypeLigne.FRAIS_INSCRIPTION);
        Versement v = Versement.creer(new BigDecimal("50000"), LocalDate.now(),
                List.of(MoyenPaiement.comptant()));

        dossier.effectuerVersement(ligne.getId(), v);

        assertThat(ligne.getStatut()).isEqualTo(StatutLigne.PAYE);
    }

    @Test
    void versement_frais_inscription_passe_statut_dossier_a_ACTIF() {
        LignePaiement ligne = ligneParType(TypeLigne.FRAIS_INSCRIPTION);
        Versement v = Versement.creer(new BigDecimal("50000"), LocalDate.now(),
                List.of(MoyenPaiement.comptant()));

        dossier.effectuerVersement(ligne.getId(), v);

        assertThat(dossier.getStatut()).isEqualTo(StatutDossier.ACTIF);
    }

    @Test
    void versement_partiel_frais_inscription_ligne_reste_APAYER() {
        LignePaiement ligne = ligneParType(TypeLigne.FRAIS_INSCRIPTION);
        Versement v = Versement.creer(new BigDecimal("20000"), LocalDate.now(),
                List.of(MoyenPaiement.comptant()));

        dossier.effectuerVersement(ligne.getId(), v);

        assertThat(ligne.getStatut()).isEqualTo(StatutLigne.APAYER);
        assertThat(ligne.getMontantRestant()).isEqualByComparingTo("30000");
    }

    // ── Ordre de règlement mensualités ────────────────────────────────────────

    @Test
    void payer_mensualite_juin_sans_payer_la_precedente_leve_exception() {
        // Mensualité octobre (ordre 2) sans avoir payé juin (ordre 1)
        LignePaiement octobre = mensualiteParOrdre(2);
        Versement v = Versement.creer(new BigDecimal("25000"), LocalDate.now(),
                List.of(MoyenPaiement.comptant()));

        assertThatThrownBy(() -> dossier.effectuerVersement(octobre.getId(), v))
                .isInstanceOf(PaiementDomainException.class);
    }

    @Test
    void payer_mensualites_dans_ordre_fonctionne() {
        LignePaiement juin = mensualiteParOrdre(1);
        payer(juin, "25000");

        LignePaiement octobre = mensualiteParOrdre(2);
        payer(octobre, "25000");

        assertThat(juin.getStatut()).isEqualTo(StatutLigne.PAYE);
        assertThat(octobre.getStatut()).isEqualTo(StatutLigne.PAYE);
    }

    @Test
    void avance_deux_mois_sans_payer_le_premier_leve_exception() {
        // Essayer de payer juillet (ordre 3) sans avoir payé juin (1) ni oct (2)
        LignePaiement novembre = mensualiteParOrdre(3);
        Versement v = Versement.creer(new BigDecimal("25000"), LocalDate.now(),
                List.of(MoyenPaiement.comptant()));

        assertThatThrownBy(() -> dossier.effectuerVersement(novembre.getId(), v))
                .isInstanceOf(PaiementDomainException.class);
    }

    // ── Distribution automatique ──────────────────────────────────────────────
    // Setup: fraisInscription=50000, autresFrais=10000, mensualite=25000
    // Minimum = 85000 | Total année = 285000

    @Test
    void distribuer_minimum_exact_couvre_frais_autres_et_mois1() {
        // 50000 + 10000 + 25000 = 85000 (minimum)
        dossier.distribuerVersement(new BigDecimal("85000"), LocalDate.now(),
                List.of(MoyenPaiement.comptant()));

        assertThat(ligneParType(TypeLigne.FRAIS_INSCRIPTION).getStatut()).isEqualTo(StatutLigne.PAYE);
        assertThat(ligneParType(TypeLigne.AUTRES_FRAIS).getStatut()).isEqualTo(StatutLigne.PAYE);
        assertThat(mensualiteParOrdre(1).getStatut()).isEqualTo(StatutLigne.PAYE);
    }

    @Test
    void distribuer_surplus_cascade_plusieurs_mois() {
        // 85000 + 5*25000 = 210000 → frais + autres + mois1 + mois2 + mois3 + mois4 + mois5
        dossier.distribuerVersement(new BigDecimal("210000"), LocalDate.now(),
                List.of(MoyenPaiement.comptant()));

        assertThat(ligneParType(TypeLigne.FRAIS_INSCRIPTION).getStatut()).isEqualTo(StatutLigne.PAYE);
        assertThat(ligneParType(TypeLigne.AUTRES_FRAIS).getStatut()).isEqualTo(StatutLigne.PAYE);
        for (int i = 1; i <= 5; i++) {
            assertThat(mensualiteParOrdre(i).getStatut()).isEqualTo(StatutLigne.PAYE);
        }
        // mois 6 non payé
        assertThat(mensualiteParOrdre(6).getStatut()).isEqualTo(StatutLigne.APAYER);
    }

    @Test
    void distribuer_avance_partielle_sur_mois() {
        // 85000 + 10000 = 95000 → frais + autres + mois1 + 10000 sur mois2 (restant 15000)
        dossier.distribuerVersement(new BigDecimal("95000"), LocalDate.now(),
                List.of(MoyenPaiement.comptant()));

        assertThat(mensualiteParOrdre(1).getStatut()).isEqualTo(StatutLigne.PAYE);
        assertThat(mensualiteParOrdre(2).getStatut()).isEqualTo(StatutLigne.APAYER);
        assertThat(mensualiteParOrdre(2).getMontantRestant()).isEqualByComparingTo("15000");
    }

    @Test
    void distribuer_inferieur_au_minimum_leve_exception() {
        // 80000 < 85000 minimum
        assertThatThrownBy(() -> dossier.distribuerVersement(new BigDecimal("80000"), LocalDate.now(),
                List.of(MoyenPaiement.comptant())))
                .isInstanceOf(PaiementDomainException.class)
                .hasMessageContaining("minimum");
    }

    @Test
    void distribuer_depasse_total_restant_leve_exception() {
        // Total = 285000, verser 300000 → interdit
        assertThatThrownBy(() -> dossier.distribuerVersement(new BigDecimal("300000"), LocalDate.now(),
                List.of(MoyenPaiement.comptant())))
                .isInstanceOf(PaiementDomainException.class)
                .hasMessageContaining("dépasse");
    }

    @Test
    void distribuer_apres_premier_versement_pas_de_minimum() {
        // Premier versement minimum
        dossier.distribuerVersement(new BigDecimal("85000"), LocalDate.now(),
                List.of(MoyenPaiement.comptant()));
        // Deuxième versement : peut verser moins d'une mensualité complète
        assertThatCode(() -> dossier.distribuerVersement(new BigDecimal("5000"), LocalDate.now(),
                List.of(MoyenPaiement.comptant())))
                .doesNotThrowAnyException();
    }

    @Test
    void distribuer_plafond_respecte_apres_premier_versement() {
        // Reste = 285000 - 85000 = 200000, verser 201000 → interdit
        dossier.distribuerVersement(new BigDecimal("85000"), LocalDate.now(),
                List.of(MoyenPaiement.comptant()));

        assertThatThrownBy(() -> dossier.distribuerVersement(new BigDecimal("201000"), LocalDate.now(),
                List.of(MoyenPaiement.comptant())))
                .isInstanceOf(PaiementDomainException.class)
                .hasMessageContaining("dépasse");
    }

    @Test
    void distribuer_montant_nul_leve_exception() {
        assertThatThrownBy(() -> dossier.distribuerVersement(BigDecimal.ZERO, LocalDate.now(),
                List.of(MoyenPaiement.comptant())))
                .isInstanceOf(PaiementDomainException.class);
    }

    @Test
    void distribuer_cloture_le_dossier_si_tout_paye() {
        BigDecimal total = dossier.getLignes().stream()
                .map(LignePaiement::getMontantDu)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        dossier.distribuerVersement(total, LocalDate.now(), List.of(MoyenPaiement.comptant()));

        assertThat(dossier.getStatut()).isEqualTo(StatutDossier.CLOTURE);
    }

    // ── Clôture ───────────────────────────────────────────────────────────────

    @Test
    void dossier_cloture_quand_toutes_lignes_payees() {
        dossier.getLignes().forEach(l ->
                payer(l, l.getMontantDu().toPlainString())
        );
        assertThat(dossier.getStatut()).isEqualTo(StatutDossier.CLOTURE);
    }

    @Test
    void versement_sur_dossier_cloture_leve_exception() {
        dossier.getLignes().forEach(l -> payer(l, l.getMontantDu().toPlainString()));
        LignePaiement ligne = ligneParType(TypeLigne.FRAIS_INSCRIPTION);

        assertThatThrownBy(() -> payer(ligne, "1"))
                .isInstanceOf(PaiementDomainException.class)
                .hasMessageContaining("clôturé");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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

    private void payer(LignePaiement ligne, String montant) {
        Versement v = Versement.creer(new BigDecimal(montant), LocalDate.now(),
                List.of(MoyenPaiement.comptant()));
        dossier.effectuerVersement(ligne.getId(), v);
    }
}
