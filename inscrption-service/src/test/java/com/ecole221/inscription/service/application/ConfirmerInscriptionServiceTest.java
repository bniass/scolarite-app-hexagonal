package com.ecole221.inscription.service.application;

import com.ecole221.inscription.service.application.port.out.InscriptionRepository;
import com.ecole221.inscription.service.application.usecase.ConfirmerInscriptionService;
import com.ecole221.inscription.service.domain.exception.InscriptionException;
import com.ecole221.inscription.service.domain.model.Inscription;
import com.ecole221.inscription.service.domain.valueobject.StatutInscription;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmerInscriptionServiceTest {

    @Mock private InscriptionRepository repository;
    @InjectMocks private ConfirmerInscriptionService service;

    private Inscription inscriptionPending() {
        return Inscription.creer(UUID.randomUUID(), UUID.randomUUID(), "2024-2025",
                new BigDecimal("50000"), new BigDecimal("25000"), new BigDecimal("10000"),
                "[]", new BigDecimal("50000"), "COMPTANT", "", "", "", "");
    }

    @Test
    void executer_confirme_inscription_PENDING() {
        UUID id = UUID.randomUUID();
        Inscription inscription = inscriptionPending();
        when(repository.trouverParId(id)).thenReturn(Optional.of(inscription));
        when(repository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));

        service.executer(id);

        assertThat(inscription.getStatut()).isEqualTo(StatutInscription.CONFIRMEE);
        verify(repository).sauvegarder(inscription);
    }

    @Test
    void executer_leve_exception_si_inscription_introuvable() {
        UUID id = UUID.randomUUID();
        when(repository.trouverParId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.executer(id))
                .isInstanceOf(InscriptionException.class);
    }
}
