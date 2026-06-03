package com.ecole221.inscription.service.application;

import com.ecole221.inscription.service.application.command.CreerInscriptionCommand;
import com.ecole221.inscription.service.application.port.out.*;
import com.ecole221.inscription.service.application.port.out.repository.AnneeAcademiqueProjectionRepository;
import com.ecole221.inscription.service.application.usecase.CreerInscriptionService;
import com.ecole221.inscription.service.domain.exception.InscriptionException;
import com.ecole221.inscription.service.domain.model.Inscription;
import com.ecole221.inscription.service.domain.model.projection.AnneeAcademiqueProjection;
import com.ecole221.inscription.service.domain.valueobject.EtatAnnee;
import com.ecole221.inscription.service.domain.valueobject.StatutInscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreerInscriptionServiceTest {

    @Mock private InscriptionRepository inscriptionRepository;
    @Mock private InscriptionOutboxPort outboxPort;
    @Mock private AnneeAcademiqueProjectionRepository anneeProjectionRepo;
    @Mock private SchoolServicePort schoolServicePort;
    @Mock private EtudiantServicePort etudiantServicePort;

    @InjectMocks private CreerInscriptionService service;

    private static final UUID ETUDIANT_ID = UUID.randomUUID();
    private static final UUID CLASSE_ID   = UUID.randomUUID();
    private static final String CODE_ANNEE = "2024-2025";

    private AnneeAcademiqueProjection projectionOuverte;
    private TarifActifResult tarif;

    @BeforeEach
    void setUp() {
        projectionOuverte = new AnneeAcademiqueProjection(
                new com.ecole221.inscription.service.domain.valueobject.CodeAnnee(CODE_ANNEE),
                EtatAnnee.INSCRIPTIONS_OUVERTES,
                "[{\"mois\":9,\"annee\":2024}]"
        );
        tarif = new TarifActifResult(UUID.randomUUID(),
                new BigDecimal("50000"), new BigDecimal("25000"), new BigDecimal("10000"));
    }

    /** Commande étudiant existant */
    private CreerInscriptionCommand commandEtudiantExistant() {
        return new CreerInscriptionCommand(
                ETUDIANT_ID, null, null, null, null,
                CLASSE_ID, CODE_ANNEE,
                new BigDecimal("50000"), "COMPTANT", "", "", "", "");
    }

    /** Commande nouvel étudiant */
    private CreerInscriptionCommand commandNouvelEtudiant() {
        return new CreerInscriptionCommand(
                null, "Diallo", "Mamadou", LocalDate.of(2000, 5, 15), "mamadou@ecole.sn",
                CLASSE_ID, CODE_ANNEE,
                new BigDecimal("50000"), "COMPTANT", "", "", "", "");
    }

    // ── Étudiant existant ─────────────────────────────────────────────────────

    @Test
    void executer_etudiant_existant_cree_inscription_PENDING() {
        when(anneeProjectionRepo.findByCodeAnnee(CODE_ANNEE)).thenReturn(Optional.of(projectionOuverte));
        when(schoolServicePort.getTarifActif(CLASSE_ID)).thenReturn(tarif);
        when(inscriptionRepository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));

        Inscription result = service.executer(commandEtudiantExistant());

        assertThat(result.getStatut()).isEqualTo(StatutInscription.PENDING);
        assertThat(result.getEtudiantId()).isEqualTo(ETUDIANT_ID);
        verify(inscriptionRepository).sauvegarder(any());
        verify(outboxPort).sauvegarder(any());
        verifyNoInteractions(etudiantServicePort);
    }

    @Test
    void executer_appelle_school_service_pour_tarif() {
        when(anneeProjectionRepo.findByCodeAnnee(CODE_ANNEE)).thenReturn(Optional.of(projectionOuverte));
        when(schoolServicePort.getTarifActif(CLASSE_ID)).thenReturn(tarif);
        when(inscriptionRepository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));

        service.executer(commandEtudiantExistant());

        verify(schoolServicePort).getTarifActif(CLASSE_ID);
    }

    // ── Nouvel étudiant ───────────────────────────────────────────────────────

    @Test
    void executer_nouvel_etudiant_appelle_etudiant_service() {
        UUID nouvelId = UUID.randomUUID();
        when(anneeProjectionRepo.findByCodeAnnee(CODE_ANNEE)).thenReturn(Optional.of(projectionOuverte));
        when(schoolServicePort.getTarifActif(CLASSE_ID)).thenReturn(tarif);
        when(etudiantServicePort.creerEtudiant(any(), any(), any(), any(), any())).thenReturn(nouvelId);
        when(inscriptionRepository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));

        Inscription result = service.executer(commandNouvelEtudiant());

        verify(etudiantServicePort).creerEtudiant(
                "Diallo", "Mamadou", LocalDate.of(2000, 5, 15), "mamadou@ecole.sn", CODE_ANNEE);
        assertThat(result.getEtudiantId()).isEqualTo(nouvelId);
        assertThat(result.getStatut()).isEqualTo(StatutInscription.PENDING);
    }

    @Test
    void executer_nouvel_etudiant_publie_event_outbox() {
        when(anneeProjectionRepo.findByCodeAnnee(CODE_ANNEE)).thenReturn(Optional.of(projectionOuverte));
        when(schoolServicePort.getTarifActif(CLASSE_ID)).thenReturn(tarif);
        when(etudiantServicePort.creerEtudiant(any(), any(), any(), any(), any())).thenReturn(UUID.randomUUID());
        when(inscriptionRepository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));

        service.executer(commandNouvelEtudiant());

        verify(outboxPort).sauvegarder(any());
    }

    // ── Cas d'erreur ──────────────────────────────────────────────────────────

    @Test
    void executer_leve_exception_si_annee_introuvable() {
        when(anneeProjectionRepo.findByCodeAnnee(CODE_ANNEE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.executer(commandEtudiantExistant()))
                .isInstanceOf(InscriptionException.class)
                .hasMessageContaining(CODE_ANNEE);
    }

    @Test
    void executer_leve_exception_si_inscriptions_non_ouvertes() {
        AnneeAcademiqueProjection fermee = new AnneeAcademiqueProjection(
                new com.ecole221.inscription.service.domain.valueobject.CodeAnnee(CODE_ANNEE),
                EtatAnnee.BROUILLON, "[]");
        when(anneeProjectionRepo.findByCodeAnnee(CODE_ANNEE)).thenReturn(Optional.of(fermee));

        assertThatThrownBy(() -> service.executer(commandEtudiantExistant()))
                .isInstanceOf(InscriptionException.class)
                .hasMessageContaining("ouvertes");
    }
}
