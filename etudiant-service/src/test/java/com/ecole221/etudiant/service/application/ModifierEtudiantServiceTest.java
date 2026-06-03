package com.ecole221.etudiant.service.application;

import com.ecole221.etudiant.service.application.command.ModifierEtudiantCommand;
import com.ecole221.etudiant.service.application.port.out.EtudiantRepository;
import com.ecole221.etudiant.service.application.port.out.OutboxPort;
import com.ecole221.etudiant.service.application.usecase.ModifierEtudiantService;
import com.ecole221.etudiant.service.domain.exception.EtudiantNotFoundException;
import com.ecole221.etudiant.service.domain.model.Etudiant;
import com.ecole221.etudiant.service.domain.valueobject.EtudiantId;
import com.ecole221.etudiant.service.domain.valueobject.Matricule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModifierEtudiantServiceTest {

    @Mock private EtudiantRepository repository;
    @Mock private OutboxPort outboxPort;
    @InjectMocks private ModifierEtudiantService service;

    private static final LocalDate DATE = LocalDate.of(2000, 5, 15);

    private Etudiant etudiantExistant() {
        return Etudiant.reconstituer(
                new EtudiantId(UUID.randomUUID()),
                new Matricule("M00001-2425"),
                "Diallo", "Mamadou", DATE, "mamadou@ecole.sn"
        );
    }

    @Test
    void executer_modifie_letudiant() {
        Etudiant etudiant = etudiantExistant();
        when(repository.trouverParMatricule("M00001-2425")).thenReturn(Optional.of(etudiant));
        when(repository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));

        service.executer(new ModifierEtudiantCommand("M00001-2425", "Sow", "Aissatou",
                LocalDate.of(1999, 3, 10)));

        assertThat(etudiant.getNom()).isEqualTo("Sow");
        assertThat(etudiant.getPrenom()).isEqualTo("Aissatou");
    }

    @Test
    void executer_sauvegarde_apres_modification() {
        when(repository.trouverParMatricule(any())).thenReturn(Optional.of(etudiantExistant()));
        when(repository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));

        service.executer(new ModifierEtudiantCommand("M00001-2425", "Sow", "Aissatou", DATE));

        verify(repository).sauvegarder(any(Etudiant.class));
    }

    @Test
    void executer_publie_event_outbox() {
        when(repository.trouverParMatricule(any())).thenReturn(Optional.of(etudiantExistant()));
        when(repository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));

        service.executer(new ModifierEtudiantCommand("M00001-2425", "Sow", "Aissatou", DATE));

        verify(outboxPort).sauvegarder(any());
    }

    @Test
    void executer_leve_exception_si_matricule_introuvable() {
        when(repository.trouverParMatricule(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.executer(
                new ModifierEtudiantCommand("INCONNU", "Sow", "Aissatou", DATE)))
                .isInstanceOf(EtudiantNotFoundException.class);
    }
}
