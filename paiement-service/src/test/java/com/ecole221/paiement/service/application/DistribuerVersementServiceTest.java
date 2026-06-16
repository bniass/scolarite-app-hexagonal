package com.ecole221.paiement.service.application;

import com.ecole221.paiement.service.application.command.DistribuerVersementCommand;
import com.ecole221.paiement.service.application.port.out.DossierPaiementRepository;
import com.ecole221.paiement.service.application.port.out.PaiementOutboxPort;
import com.ecole221.paiement.service.application.usecase.DistribuerVersementService;
import com.ecole221.paiement.service.domain.event.DossierInitialiseEvent;
import com.ecole221.paiement.service.domain.exception.PaiementDomainException;
import com.ecole221.paiement.service.domain.model.*;
import com.ecole221.paiement.service.domain.valueobject.StatutDossier;
import com.ecole221.paiement.service.domain.valueobject.StatutLigne;
import com.ecole221.paiement.service.domain.valueobject.TypeLigne;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DistribuerVersementServiceTest {

    @Mock private DossierPaiementRepository repository;
    @Mock private PaiementOutboxPort outboxPort;
    @InjectMocks private DistribuerVersementService service;

    // fraisInscription=50000, autresFrais=10000, mensualite=25000
    // Minimum premier versement = 85000 | Total = 285000
    private static final BigDecimal FRAIS        = new BigDecimal("50000");
    private static final BigDecimal MENSUALITE   = new BigDecimal("25000");
    private static final BigDecimal AUTRES_FRAIS = new BigDecimal("10000");
    private static final BigDecimal MINIMUM      = new BigDecimal("85000");

    private static final List<MoisAcademique> MOIS = List.of(
            new MoisAcademique(9, 2024), new MoisAcademique(10, 2024),
            new MoisAcademique(11, 2024), new MoisAcademique(12, 2024),
            new MoisAcademique(1, 2025), new MoisAcademique(2, 2025),
            new MoisAcademique(3, 2025), new MoisAcademique(4, 2025),
            new MoisAcademique(5, 2025)
    );

    private DossierPaiement dossier;
    private UUID inscriptionId;

    @BeforeEach
    void setUp() {
        inscriptionId = UUID.randomUUID();
        dossier = DossierPaiement.initialiser(
                inscriptionId, UUID.randomUUID(), UUID.randomUUID(), "2024-2025",
                FRAIS, MENSUALITE, AUTRES_FRAIS, MOIS
        );
        when(repository.trouverParInscriptionId(inscriptionId)).thenReturn(Optional.of(dossier));
        when(repository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private DistribuerVersementCommand commandAvec(String montant) {
        return new DistribuerVersementCommand(
                inscriptionId, new BigDecimal(montant),
                LocalDate.now(), MoyenPaiement.comptant());
    }

    // ── Premier versement (statut INITIALISE) ─────────────────────────────────

    @Test
    void executer_premier_versement_minimum_passe_dossier_a_ACTIF() {
        DossierPaiement result = service.executer(commandAvec("85000"));
        assertThat(result.getStatut()).isEqualTo(StatutDossier.ACTIF);
    }

    @Test
    void executer_premier_versement_couvre_frais_et_autres_et_mois1() {
        DossierPaiement result = service.executer(commandAvec("85000"));

        assertThat(result.getLignes().stream()
                .filter(l -> l.getType() == TypeLigne.FRAIS_INSCRIPTION)
                .findFirst().orElseThrow().getStatut()).isEqualTo(StatutLigne.PAYE);
        assertThat(result.getLignes().stream()
                .filter(l -> l.getType() == TypeLigne.AUTRES_FRAIS)
                .findFirst().orElseThrow().getStatut()).isEqualTo(StatutLigne.PAYE);
    }

    @Test
    void executer_premier_versement_inferieur_au_minimum_leve_exception() {
        // 80000 < 85000 minimum
        assertThatThrownBy(() -> service.executer(commandAvec("80000")))
                .isInstanceOf(PaiementDomainException.class)
                .hasMessageContaining("minimum");
    }

    @Test
    void executer_premier_versement_mobile_money_accepte() {
        DistribuerVersementCommand cmd = new DistribuerVersementCommand(
                inscriptionId, MINIMUM, LocalDate.now(),
                MoyenPaiement.mobileMoney("Orange", "OM-123456"));
        assertThatCode(() -> service.executer(cmd)).doesNotThrowAnyException();
    }

    @Test
    void executer_premier_versement_banque_accepte() {
        DistribuerVersementCommand cmd = new DistribuerVersementCommand(
                inscriptionId, MINIMUM, LocalDate.now(),
                MoyenPaiement.banque("SGBS", "VIR-001"));
        assertThatCode(() -> service.executer(cmd)).doesNotThrowAnyException();
    }

    // ── Versements suivants ───────────────────────────────────────────────────

    @Test
    void executer_cascade_sur_plusieurs_mensualites() {
        // Premier versement puis cascade de 3 mensualités supplémentaires
        service.executer(commandAvec("85000"));

        // Reset mock pour second appel
        DossierPaiement dossierActif = dossier;
        when(repository.trouverParInscriptionId(inscriptionId)).thenReturn(Optional.of(dossierActif));

        // 3 mensualités supplémentaires = 75000
        DossierPaiement result = service.executer(commandAvec("75000"));

        long nbMoisPayees = result.getLignes().stream()
                .filter(l -> l.getType() == TypeLigne.MENSUALITE && l.estPayee())
                .count();
        assertThat(nbMoisPayees).isEqualTo(4);
    }

    @Test
    void executer_versement_depasse_total_restant_leve_exception() {
        // 300000 > 285000 total
        assertThatThrownBy(() -> service.executer(commandAvec("300000")))
                .isInstanceOf(PaiementDomainException.class)
                .hasMessageContaining("dépasse");
    }

    // ── Persistance ───────────────────────────────────────────────────────────

    @Test
    void executer_sauvegarde_le_dossier() {
        service.executer(commandAvec("85000"));
        verify(repository).sauvegarder(any(DossierPaiement.class));
    }

    @Test
    void executer_premier_versement_publie_event_confirmation_inscription() {
        service.executer(commandAvec("85000"));
        verify(outboxPort).sauvegarder(any(DossierInitialiseEvent.class));
    }

    @Test
    void executer_versement_suivant_ne_publie_pas_nouvel_event() {
        // Premier versement
        service.executer(commandAvec("85000"));
        // Deuxième versement — plus d'event de confirmation
        service.executer(commandAvec("25000"));
        verify(outboxPort, times(1)).sauvegarder(any());
    }

    @Test
    void executer_leve_exception_si_dossier_introuvable() {
        when(repository.trouverParInscriptionId(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.executer(new DistribuerVersementCommand(
                UUID.randomUUID(), MINIMUM, LocalDate.now(), MoyenPaiement.comptant())))
                .isInstanceOf(PaiementDomainException.class)
                .hasMessageContaining("introuvable");
    }
}
