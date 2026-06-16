package com.ecole221.inscription.service.application;

import com.ecole221.inscription.service.application.port.out.InscriptionOutboxPort;
import com.ecole221.inscription.service.application.port.out.InscriptionRepository;
import com.ecole221.inscription.service.application.usecase.AnnulerInscriptionAdminService;
import com.ecole221.inscription.service.domain.exception.InscriptionException;
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
class AnnulerInscriptionAdminServiceTest {

    @Mock private InscriptionRepository repository;
    @Mock private InscriptionOutboxPort outboxPort;
    @InjectMocks private AnnulerInscriptionAdminService service;

    private Inscription inscriptionAvecStatut(StatutInscription statut) {
        return Inscription.reconstituer(
                UUID.randomUUID(), UUID.randomUUID(), false, UUID.randomUUID(),
                "2024-2025",
                new BigDecimal("50000"), new BigDecimal("25000"), new BigDecimal("10000"),
                "[]", statut, LocalDateTime.now(), null, null
        );
    }

    @Test
    void executer_annule_inscription_PENDING() {
        Inscription inscription = inscriptionAvecStatut(StatutInscription.PENDING);
        when(repository.trouverParId(inscription.getId())).thenReturn(Optional.of(inscription));
        when(repository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));

        service.executer(inscription.getId(), "Erreur de saisie");

        assertThat(inscription.getStatut()).isEqualTo(StatutInscription.ANNULEE);
        assertThat(inscription.getMotifAnnulation()).isEqualTo("Erreur de saisie");
        assertThat(inscription.getAnnuleLe()).isNotNull();
    }

    @Test
    void executer_sauvegarde_inscription_annulee() {
        Inscription inscription = inscriptionAvecStatut(StatutInscription.PENDING);
        when(repository.trouverParId(inscription.getId())).thenReturn(Optional.of(inscription));
        when(repository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));

        service.executer(inscription.getId(), "motif");

        verify(repository).sauvegarder(inscription);
    }

    @Test
    void executer_refuse_si_inscription_CONFIRMEE() {
        // CONFIRMEE = au moins un versement → annulation impossible
        Inscription inscription = inscriptionAvecStatut(StatutInscription.CONFIRMEE);
        when(repository.trouverParId(inscription.getId())).thenReturn(Optional.of(inscription));

        assertThatThrownBy(() -> service.executer(inscription.getId(), "motif"))
                .isInstanceOf(InscriptionException.class)
                .hasMessageContaining("versement");
    }

    @Test
    void executer_refuse_si_inscription_deja_ANNULEE() {
        Inscription inscription = inscriptionAvecStatut(StatutInscription.PENDING);
        when(repository.trouverParId(inscription.getId())).thenReturn(Optional.of(inscription));
        when(repository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));
        service.executer(inscription.getId(), "premier motif");

        when(repository.trouverParId(inscription.getId())).thenReturn(Optional.of(inscription));

        assertThatThrownBy(() -> service.executer(inscription.getId(), "second motif"))
                .isInstanceOf(InscriptionException.class)
                .hasMessageContaining("déjà annulée");
    }

    @Test
    void executer_leve_exception_si_inscription_introuvable() {
        when(repository.trouverParId(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.executer(UUID.randomUUID(), "motif"))
                .isInstanceOf(InscriptionNotFoundException.class);
    }
}
