package com.ecole221.school.service.application;

import com.ecole221.school.service.application.command.AffecterTarifCommand;
import com.ecole221.school.service.application.port.out.outbox.OutboxPort;
import com.ecole221.school.service.application.port.out.repository.ClasseRepository;
import com.ecole221.school.service.application.port.out.repository.TarifRepository;
import com.ecole221.school.service.application.usecase.AffecterTarifService;
import com.ecole221.school.service.domain.exception.SchoolNotFoundException;
import com.ecole221.school.service.domain.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AffecterTarifServiceTest {

    @Mock private ClasseRepository classeRepository;
    @Mock private TarifRepository tarifRepository;
    @Mock private OutboxPort outboxPort;
    @InjectMocks private AffecterTarifService service;

    private static final UUID CLASSE_UUID = UUID.randomUUID();
    private static final UUID TARIF_UUID  = UUID.randomUUID();

    private Classe classeExistante() {
        return Classe.reconstituer(ClasseId.of(CLASSE_UUID), "L1-INFO", "L1 Informatique",
                Cycle.LICENCE, Niveau.L1, FiliereId.of(UUID.randomUUID()), List.of());
    }

    private Tarif tarifExistant() {
        return Tarif.reconstituer(TarifId.of(TARIF_UUID),
                new BigDecimal("50000"), new BigDecimal("25000"), new BigDecimal("10000"));
    }

    @Test
    void executer_affecte_le_tarif_a_la_classe() {
        when(classeRepository.findById(any())).thenReturn(Optional.of(classeExistante()));
        when(tarifRepository.findById(any())).thenReturn(Optional.of(tarifExistant()));

        service.executer(new AffecterTarifCommand(CLASSE_UUID, TARIF_UUID));

        verify(classeRepository).save(any(Classe.class));
    }

    @Test
    void executer_publie_event_outbox() {
        when(classeRepository.findById(any())).thenReturn(Optional.of(classeExistante()));
        when(tarifRepository.findById(any())).thenReturn(Optional.of(tarifExistant()));

        service.executer(new AffecterTarifCommand(CLASSE_UUID, TARIF_UUID));

        verify(outboxPort).save(any());
    }

    @Test
    void executer_leve_exception_si_classe_introuvable() {
        when(classeRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.executer(new AffecterTarifCommand(CLASSE_UUID, TARIF_UUID)))
                .isInstanceOf(SchoolNotFoundException.class)
                .hasMessageContaining(CLASSE_UUID.toString());
    }

    @Test
    void executer_leve_exception_si_tarif_introuvable() {
        when(classeRepository.findById(any())).thenReturn(Optional.of(classeExistante()));
        when(tarifRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.executer(new AffecterTarifCommand(CLASSE_UUID, TARIF_UUID)))
                .isInstanceOf(SchoolNotFoundException.class)
                .hasMessageContaining(TARIF_UUID.toString());
    }
}
