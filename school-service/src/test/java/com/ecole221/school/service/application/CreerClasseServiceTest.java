package com.ecole221.school.service.application;

import com.ecole221.school.service.application.command.CreerClasseCommand;
import com.ecole221.school.service.application.port.out.outbox.OutboxPort;
import com.ecole221.school.service.application.port.out.repository.ClasseRepository;
import com.ecole221.school.service.application.port.out.repository.FiliereRepository;
import com.ecole221.school.service.application.usecase.CreerClasseService;
import com.ecole221.school.service.domain.exception.SchoolException;
import com.ecole221.school.service.domain.exception.SchoolNotFoundException;
import com.ecole221.school.service.domain.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreerClasseServiceTest {

    @Mock private ClasseRepository classeRepository;
    @Mock private FiliereRepository filiereRepository;
    @Mock private OutboxPort outboxPort;
    @InjectMocks private CreerClasseService service;

    private static final UUID FILIERE_UUID = UUID.randomUUID();

    private CreerClasseCommand commandValide() {
        return new CreerClasseCommand("L1-INFO", "L1 Informatique", Cycle.LICENCE, Niveau.L1, FILIERE_UUID);
    }

    private void mockFiliereExistante() {
        when(filiereRepository.findById(any())).thenReturn(Optional.of(
                Filiere.reconstituer(FiliereId.of(FILIERE_UUID), "INFO", "Informatique")));
    }

    @Test
    void executer_retourne_un_uuid() {
        when(classeRepository.findByCode(any())).thenReturn(Optional.empty());
        mockFiliereExistante();

        UUID id = service.executer(commandValide());

        assertThat(id).isNotNull();
    }

    @Test
    void executer_sauvegarde_la_classe() {
        when(classeRepository.findByCode(any())).thenReturn(Optional.empty());
        mockFiliereExistante();

        service.executer(commandValide());

        verify(classeRepository).save(any(Classe.class));
    }

    @Test
    void executer_publie_event_outbox() {
        when(classeRepository.findByCode(any())).thenReturn(Optional.empty());
        mockFiliereExistante();

        service.executer(commandValide());

        verify(outboxPort).save(any());
    }

    @Test
    void executer_leve_exception_si_code_classe_deja_existant() {
        when(classeRepository.findByCode("L1-INFO")).thenReturn(Optional.of(
                Classe.reconstituer(ClasseId.generate(), "L1-INFO", "L1 Info",
                        Cycle.LICENCE, Niveau.L1, FiliereId.of(FILIERE_UUID), java.util.List.of())));

        assertThatThrownBy(() -> service.executer(commandValide()))
                .isInstanceOf(SchoolException.class)
                .hasMessageContaining("L1-INFO");
    }

    @Test
    void executer_leve_exception_si_filiere_introuvable() {
        when(classeRepository.findByCode(any())).thenReturn(Optional.empty());
        when(filiereRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.executer(commandValide()))
                .isInstanceOf(SchoolNotFoundException.class);
    }
}
