package com.ecole221.inscription.service.domain;

import com.ecole221.inscription.service.domain.event.InscriptionCreeeEvent;
import com.ecole221.inscription.service.domain.exception.InscriptionException;
import com.ecole221.inscription.service.domain.model.Inscription;
import com.ecole221.inscription.service.domain.valueobject.StatutInscription;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class InscriptionDomainTest {

    private static final UUID ETUDIANT_ID = UUID.randomUUID();
    private static final UUID CLASSE_ID   = UUID.randomUUID();
    private static final String CODE_ANNEE = "2024-2025";
    private static final BigDecimal FRAIS  = new BigDecimal("50000");
    private static final BigDecimal MENS   = new BigDecimal("25000");
    private static final BigDecimal AUTRES = new BigDecimal("10000");
    private static final String MOIS_JSON  = "[{\"mois\":9,\"annee\":2024}]";

    private Inscription creer() {
        return Inscription.creer(ETUDIANT_ID, false, CLASSE_ID, CODE_ANNEE,
                FRAIS, MENS, AUTRES, MOIS_JSON);
    }

    // ── Création ──────────────────────────────────────────────────────────────

    @Test
    void creer_statut_est_PENDING() {
        assertThat(creer().getStatut()).isEqualTo(StatutInscription.PENDING);
    }

    @Test
    void creer_genere_un_event_InscriptionCreee() {
        Inscription i = creer();
        var events = i.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(InscriptionCreeeEvent.class);
    }

    @Test
    void creer_event_porte_les_bonnes_donnees() {
        Inscription i = creer();
        InscriptionCreeeEvent event = (InscriptionCreeeEvent) i.pullDomainEvents().get(0);
        assertThat(event.getEtudiantId()).isEqualTo(ETUDIANT_ID);
        assertThat(event.getClasseId()).isEqualTo(CLASSE_ID);
        assertThat(event.getFraisInscription()).isEqualByComparingTo(FRAIS);
        assertThat(event.getMensualite()).isEqualByComparingTo(MENS);
        assertThat(event.getAutresFrais()).isEqualByComparingTo(AUTRES);
    }

    @Test
    void pullDomainEvents_vide_la_liste() {
        Inscription i = creer();
        i.pullDomainEvents();
        assertThat(i.pullDomainEvents()).isEmpty();
    }

    // ── Confirmer ─────────────────────────────────────────────────────────────

    @Test
    void confirmer_passe_statut_a_CONFIRMEE() {
        Inscription i = creer();
        i.confirmer();
        assertThat(i.getStatut()).isEqualTo(StatutInscription.CONFIRMEE);
    }

    @Test
    void confirmer_deux_fois_leve_exception() {
        Inscription i = creer();
        i.confirmer();
        assertThatThrownBy(i::confirmer).isInstanceOf(InscriptionException.class);
    }

    @Test
    void confirmer_inscription_echouee_leve_exception() {
        Inscription i = creer();
        i.echouer();
        assertThatThrownBy(i::confirmer).isInstanceOf(InscriptionException.class);
    }

    // ── Échouer ───────────────────────────────────────────────────────────────

    @Test
    void echouer_passe_statut_a_ECHOUEE() {
        Inscription i = creer();
        i.echouer();
        assertThat(i.getStatut()).isEqualTo(StatutInscription.ECHOUEE);
    }

    @Test
    void echouer_inscription_confirmee_leve_exception() {
        Inscription i = creer();
        i.confirmer();
        assertThatThrownBy(i::echouer).isInstanceOf(InscriptionException.class);
    }
}
