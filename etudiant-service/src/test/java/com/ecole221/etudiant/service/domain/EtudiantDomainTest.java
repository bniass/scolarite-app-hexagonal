package com.ecole221.etudiant.service.domain;

import com.ecole221.etudiant.service.domain.exception.EtudiantException;
import com.ecole221.etudiant.service.domain.model.Etudiant;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class EtudiantDomainTest {

    private static final LocalDate DATE_NAISSANCE = LocalDate.of(2000, 5, 15);

    private Etudiant etudiantValide() {
        return Etudiant.creer("Diallo", "Mamadou", DATE_NAISSANCE, "mamadou@ecole.sn", 1L, "2024-2025");
    }

    // ── Création ──────────────────────────────────────────────────────────────

    @Test
    void creer_etudiant_avec_donnees_valides() {
        Etudiant e = etudiantValide();

        assertThat(e.getNom()).isEqualTo("Diallo");
        assertThat(e.getPrenom()).isEqualTo("Mamadou");
        assertThat(e.getEmail()).isEqualTo("mamadou@ecole.sn");
        assertThat(e.getId()).isNotNull();
    }

    @Test
    void creer_genere_matricule_avec_bon_format() {
        Etudiant e = etudiantValide();
        // ordre=1, codeAnnee=2024-2025 → suffixe=2425 → M00001-2425
        assertThat(e.getMatricule().getValeur()).isEqualTo("M00001-2425");
    }

    @Test
    void creer_genere_un_event_EtudiantCree() {
        Etudiant e = etudiantValide();
        assertThat(e.pullDomainEvents()).hasSize(1);
    }

    @Test
    void creer_normalise_email_en_minuscules() {
        Etudiant e = Etudiant.creer("Diallo", "Mamadou", DATE_NAISSANCE, "MAMADOU@ECOLE.SN", 1L, "2024-2025");
        assertThat(e.getEmail()).isEqualTo("mamadou@ecole.sn");
    }

    @Test
    void creer_nom_vide_leve_exception() {
        assertThatThrownBy(() -> Etudiant.creer("", "Mamadou", DATE_NAISSANCE, "m@e.sn", 1L, "2024-2025"))
                .isInstanceOf(EtudiantException.class)
                .hasMessageContaining("nom");
    }

    @Test
    void creer_prenom_null_leve_exception() {
        assertThatThrownBy(() -> Etudiant.creer("Diallo", null, DATE_NAISSANCE, "m@e.sn", 1L, "2024-2025"))
                .isInstanceOf(EtudiantException.class)
                .hasMessageContaining("prénom");
    }

    @Test
    void creer_email_sans_arobase_leve_exception() {
        assertThatThrownBy(() -> Etudiant.creer("Diallo", "Mamadou", DATE_NAISSANCE, "pasdearobase", 1L, "2024-2025"))
                .isInstanceOf(EtudiantException.class)
                .hasMessageContaining("email");
    }

    @Test
    void creer_date_naissance_future_leve_exception() {
        assertThatThrownBy(() -> Etudiant.creer("Diallo", "Mamadou",
                LocalDate.now().plusDays(1), "m@e.sn", 1L, "2024-2025"))
                .isInstanceOf(EtudiantException.class)
                .hasMessageContaining("futur");
    }

    // ── Matricule / suffixeAnnee ──────────────────────────────────────────────

    @Test
    void suffixeAnnee_format_correct() {
        assertThat(Etudiant.suffixeAnnee("2024-2025")).isEqualTo("2425");
    }

    @Test
    void suffixeAnnee_format_invalide_leve_exception() {
        assertThatThrownBy(() -> Etudiant.suffixeAnnee("20242025"))
                .isInstanceOf(EtudiantException.class);
    }

    @Test
    void matricule_incremente_avec_ordre() {
        Etudiant e1 = Etudiant.creer("A", "A", DATE_NAISSANCE, "a@e.sn", 1L, "2024-2025");
        Etudiant e2 = Etudiant.creer("B", "B", DATE_NAISSANCE, "b@e.sn", 2L, "2024-2025");
        assertThat(e1.getMatricule().getValeur()).isEqualTo("M00001-2425");
        assertThat(e2.getMatricule().getValeur()).isEqualTo("M00002-2425");
    }

    // ── Modification ──────────────────────────────────────────────────────────

    @Test
    void modifier_met_a_jour_nom_prenom_dateNaissance() {
        Etudiant e = etudiantValide();
        LocalDate nouvelleDateNaissance = LocalDate.of(1999, 3, 10);

        e.modifier("Sow", "Aissatou", nouvelleDateNaissance);

        assertThat(e.getNom()).isEqualTo("Sow");
        assertThat(e.getPrenom()).isEqualTo("Aissatou");
        assertThat(e.getDateNaissance()).isEqualTo(nouvelleDateNaissance);
    }

    @Test
    void modifier_genere_event_EtudiantModifie() {
        Etudiant e = etudiantValide();
        e.pullDomainEvents(); // vider event création

        e.modifier("Sow", "Aissatou", DATE_NAISSANCE);

        assertThat(e.pullDomainEvents()).hasSize(1);
    }

    @Test
    void modifier_nom_vide_leve_exception() {
        Etudiant e = etudiantValide();
        assertThatThrownBy(() -> e.modifier("", "Mamadou", DATE_NAISSANCE))
                .isInstanceOf(EtudiantException.class);
    }
}
