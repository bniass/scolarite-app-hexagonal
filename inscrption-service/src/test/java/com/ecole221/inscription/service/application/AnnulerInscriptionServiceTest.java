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

    private Inscription inscriptionPendingAncien() {
        return Inscription.reconstituer(
                UUID.randomUUID(), UUID.randomUUID(), false, UUID.randomUUID(),
                "2024-2025",
                new BigDecimal("50000"), new BigDecimal("25000"), new BigDecimal("10000"),
                "[]", StatutInscription.PENDING, LocalDateTime.now(), null, null
        );
    }

    private Inscription inscriptionPendingNouveau() {
        return Inscription.reconstituer(
                UUID.randomUUID(), UUID.randomUUID(), true, UUID.randomUUID(),
                "2024-2025",
                new BigDecimal("50000"), new BigDecimal("25000"), new BigDecimal("10000"),
                "[]", StatutInscription.PENDING, LocalDateTime.now(), null, null
        );
    }

    @Test
    void executer_leve_exception_si_inscription_introuvable() {
        when(repository.trouverParId(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.executer(UUID.randomUUID(), "motif"))
                .isInstanceOf(InscriptionNotFoundException.class);
    }

    @Test
    void executer_supprime_inscription() {
        Inscription inscription = inscriptionPendingAncien();
        when(repository.trouverParId(inscription.getId())).thenReturn(Optional.of(inscription));

        service.executer(inscription.getId(), "paiement échoué");

        verify(repository).supprimer(inscription.getId());
    }

    @Test
    void executer_ne_supprime_pas_etudiant_si_etudiant_ancien() {
        // etudiantNouveau=false → compensation étudiant pas nécessaire
        Inscription inscription = inscriptionPendingAncien();
        when(repository.trouverParId(inscription.getId())).thenReturn(Optional.of(inscription));

        service.executer(inscription.getId(), "motif");

        verifyNoInteractions(etudiantServicePort);
    }

    @Test
    void executer_supprime_etudiant_si_nouvel_etudiant() {
        // etudiantNouveau=true → étudiant créé lors de cette inscription, on compense
        Inscription inscription = inscriptionPendingNouveau();
        UUID etudiantId = inscription.getEtudiantId();
        when(repository.trouverParId(inscription.getId())).thenReturn(Optional.of(inscription));

        service.executer(inscription.getId(), "motif");

        verify(etudiantServicePort).supprimerEtudiant(etudiantId);
    }

    @Test
    void executer_compensation_etudiant_silencieuse_si_service_down() {
        // La suppression échoue → pas d'exception propagée, inscription déjà supprimée
        Inscription inscription = inscriptionPendingNouveau();
        when(repository.trouverParId(inscription.getId())).thenReturn(Optional.of(inscription));
        doThrow(new RuntimeException("etudiant-service down"))
                .when(etudiantServicePort).supprimerEtudiant(any());

        assertThatCode(() -> service.executer(inscription.getId(), "motif"))
                .doesNotThrowAnyException();
    }
}
