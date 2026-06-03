package com.ecole221.etudiant.service.application;

import com.ecole221.etudiant.service.application.command.CreerEtudiantCommand;
import com.ecole221.etudiant.service.application.port.out.EtudiantRepository;
import com.ecole221.etudiant.service.application.port.out.OutboxPort;
import com.ecole221.etudiant.service.application.usecase.CreerEtudiantService;
import com.ecole221.etudiant.service.domain.exception.EtudiantException;
import com.ecole221.etudiant.service.domain.model.Etudiant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreerEtudiantServiceTest {

    @Mock private EtudiantRepository repository;
    @Mock private OutboxPort outboxPort;
    @InjectMocks private CreerEtudiantService service;

    private CreerEtudiantCommand commandValide() {
        return new CreerEtudiantCommand("Diallo", "Mamadou",
                LocalDate.of(2000, 5, 15), "mamadou@ecole.sn", "2024-2025");
    }

    @Test
    void executer_retourne_etudiant_cree() {
        when(repository.existeParEmail(any())).thenReturn(false);
        when(repository.compterParSuffixeAnnee(any())).thenReturn(0L);
        when(repository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));

        Etudiant result = service.executer(commandValide());

        assertThat(result).isNotNull();
        assertThat(result.getNom()).isEqualTo("Diallo");
        assertThat(result.getMatricule().getValeur()).isEqualTo("M00001-2425");
    }

    @Test
    void executer_sauvegarde_etudiant() {
        when(repository.existeParEmail(any())).thenReturn(false);
        when(repository.compterParSuffixeAnnee(any())).thenReturn(0L);
        when(repository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));

        service.executer(commandValide());

        verify(repository).sauvegarder(any(Etudiant.class));
    }

    @Test
    void executer_publie_event_outbox() {
        when(repository.existeParEmail(any())).thenReturn(false);
        when(repository.compterParSuffixeAnnee(any())).thenReturn(0L);
        when(repository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));

        service.executer(commandValide());

        verify(outboxPort).sauvegarder(any());
    }

    @Test
    void executer_leve_exception_si_email_deja_utilise() {
        when(repository.existeParEmail("mamadou@ecole.sn")).thenReturn(true);

        assertThatThrownBy(() -> service.executer(commandValide()))
                .isInstanceOf(EtudiantException.class)
                .hasMessageContaining("mamadou@ecole.sn");
    }

    @Test
    void executer_incremente_ordre_selon_suffixe_annee() {
        when(repository.existeParEmail(any())).thenReturn(false);
        when(repository.compterParSuffixeAnnee("2425")).thenReturn(5L);
        when(repository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));

        Etudiant result = service.executer(commandValide());

        // ordre = 5 + 1 = 6 → M00006-2425
        assertThat(result.getMatricule().getValeur()).isEqualTo("M00006-2425");
    }
}
