package com.ecole221.school.service.application;

import com.ecole221.school.service.application.command.CreerFiliereCommand;
import com.ecole221.school.service.application.port.out.outbox.OutboxPort;
import com.ecole221.school.service.application.port.out.repository.FiliereRepository;
import com.ecole221.school.service.application.usecase.CreerFiliereService;
import com.ecole221.school.service.domain.exception.SchoolException;
import com.ecole221.school.service.domain.model.Filiere;
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
class CreerFiliereServiceTest {

    @Mock private FiliereRepository filiereRepository;
    @Mock private OutboxPort outboxPort;
    @InjectMocks private CreerFiliereService service;

    @Test
    void executer_retourne_un_uuid() {
        when(filiereRepository.findByCode(any())).thenReturn(Optional.empty());

        UUID id = service.executer(new CreerFiliereCommand("INFO", "Informatique"));

        assertThat(id).isNotNull();
    }

    @Test
    void executer_sauvegarde_la_filiere() {
        when(filiereRepository.findByCode(any())).thenReturn(Optional.empty());

        service.executer(new CreerFiliereCommand("INFO", "Informatique"));

        verify(filiereRepository).save(any(Filiere.class));
    }

    @Test
    void executer_publie_event_outbox() {
        when(filiereRepository.findByCode(any())).thenReturn(Optional.empty());

        service.executer(new CreerFiliereCommand("INFO", "Informatique"));

        verify(outboxPort).save(any());
    }

    @Test
    void executer_leve_exception_si_code_deja_existant() {
        when(filiereRepository.findByCode("INFO")).thenReturn(Optional.of(
                Filiere.reconstituer(null, "INFO", "Informatique")));

        assertThatThrownBy(() -> service.executer(new CreerFiliereCommand("INFO", "Autre")))
                .isInstanceOf(SchoolException.class)
                .hasMessageContaining("INFO");
    }
}
