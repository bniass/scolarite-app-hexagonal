package com.ecole221.inscription.service.application;

import com.ecole221.inscription.service.application.port.out.EtudiantServicePort;
import com.ecole221.inscription.service.application.port.out.InscriptionRepository;
import com.ecole221.inscription.service.application.usecase.AnnulerInscriptionService;
import com.ecole221.inscription.service.domain.exception.InscriptionNotFoundException;
import com.ecole221.inscription.service.domain.model.Inscription;
import com.ecole221.inscription.service.domain.valueobject.StatutInscription;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnnulerInscriptionServiceTest {

    @Mock private InscriptionRepository repository;
    @Mock private EtudiantServicePort etudiantServicePort;
    @InjectMocks private AnnulerInscriptionService service;

    private Inscription inscriptionPending() {
        return Inscription.reconstituer(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "2024-2025",
                new BigDecimal("50000"), new BigDecimal("25000"), new BigDecimal("10000"),
                "[]", StatutInscription.PENDING, LocalDateTime.now()
        );
    }

    @Test
    void executer_leve_exception_si_inscription_introuvable() {
        when(repository.trouverParId(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.executer(UUID.randomUUID(), "motif"))
                .isInstanceOf(InscriptionNotFoundException.class);
    }

    @Test
    void executer_passe_statut_a_ECHOUEE() {
        Inscription inscription = inscriptionPending();
        when(repository.trouverParId(inscription.getId())).thenReturn(Optional.of(inscription));
        when(repository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));

        service.executer(inscription.getId(), "paiement échoué");

        assertThat(inscription.getStatut()).isEqualTo(StatutInscription.ECHOUEE);
    }

    @Test
    void executer_appelle_sauvegarder() {
        Inscription inscription = inscriptionPending();
        when(repository.trouverParId(inscription.getId())).thenReturn(Optional.of(inscription));
        when(repository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));

        service.executer(inscription.getId(), "motif");

        verify(repository).sauvegarder(inscription);
    }

    @Test
    void executer_supprime_etudiant_compensation_saga() {
        Inscription inscription = inscriptionPending();
        UUID etudiantId = inscription.getEtudiantId();
        when(repository.trouverParId(inscription.getId())).thenReturn(Optional.of(inscription));
        when(repository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));

        service.executer(inscription.getId(), "motif");

        verify(etudiantServicePort).supprimerEtudiant(etudiantId);
    }

    @Test
    void executer_ne_leve_pas_exception_si_suppression_etudiant_echoue() {
        Inscription inscription = inscriptionPending();
        when(repository.trouverParId(inscription.getId())).thenReturn(Optional.of(inscription));
        when(repository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("etudiant-service down"))
                .when(etudiantServicePort).supprimerEtudiant(any());

        // La compensation doit être silencieuse — pas d'exception propagée
        assertThatCode(() -> service.executer(inscription.getId(), "motif"))
                .doesNotThrowAnyException();
        assertThat(inscription.getStatut()).isEqualTo(StatutInscription.ECHOUEE);
    }
}
