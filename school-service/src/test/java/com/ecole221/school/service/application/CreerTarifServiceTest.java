package com.ecole221.school.service.application;

import com.ecole221.school.service.application.command.CreerTarifCommand;
import com.ecole221.school.service.application.port.out.outbox.OutboxPort;
import com.ecole221.school.service.application.port.out.repository.TarifRepository;
import com.ecole221.school.service.application.usecase.CreerTarifService;
import com.ecole221.school.service.domain.model.Tarif;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreerTarifServiceTest {

    @Mock private TarifRepository tarifRepository;
    @Mock private OutboxPort outboxPort;
    @InjectMocks private CreerTarifService service;

    private CreerTarifCommand commandValide() {
        return new CreerTarifCommand(
                new BigDecimal("50000"),
                new BigDecimal("25000"),
                new BigDecimal("10000")
        );
    }

    @Test
    void executer_retourne_un_uuid() {
        UUID id = service.executer(commandValide());
        assertThat(id).isNotNull();
    }

    @Test
    void executer_sauvegarde_le_tarif() {
        service.executer(commandValide());
        verify(tarifRepository).save(any(Tarif.class));
    }

    @Test
    void executer_publie_event_outbox() {
        service.executer(commandValide());
        verify(outboxPort).save(any());
    }
}
