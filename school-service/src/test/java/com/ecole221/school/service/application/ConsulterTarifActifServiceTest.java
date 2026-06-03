package com.ecole221.school.service.application;

import com.ecole221.school.service.application.port.out.repository.ClasseRepository;
import com.ecole221.school.service.application.port.out.repository.TarifRepository;
import com.ecole221.school.service.application.usecase.ConsulterTarifActifService;
import com.ecole221.school.service.application.usecase.TarifActifResult;
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
class ConsulterTarifActifServiceTest {

    @Mock private ClasseRepository classeRepository;
    @Mock private TarifRepository tarifRepository;
    @InjectMocks private ConsulterTarifActifService service;

    private static final UUID CLASSE_UUID = UUID.randomUUID();
    private static final UUID TARIF_UUID  = UUID.randomUUID();

    private Classe classeAvecTarifActif() {
        Classe classe = Classe.reconstituer(ClasseId.of(CLASSE_UUID), "L1-INFO", "L1 Informatique",
                Cycle.LICENCE, Niveau.L1, FiliereId.of(UUID.randomUUID()), List.of());
        classe.affecterTarif(TarifId.of(TARIF_UUID));
        return classe;
    }

    private Tarif tarifExistant() {
        return Tarif.reconstituer(TarifId.of(TARIF_UUID),
                new BigDecimal("50000"), new BigDecimal("25000"), new BigDecimal("10000"));
    }

    @Test
    void executer_retourne_le_tarif_actif() {
        when(classeRepository.findById(any())).thenReturn(Optional.of(classeAvecTarifActif()));
        when(tarifRepository.findById(any())).thenReturn(Optional.of(tarifExistant()));

        TarifActifResult result = service.executer(CLASSE_UUID);

        assertThat(result.tarifId()).isEqualTo(TARIF_UUID);
        assertThat(result.fraisInscription()).isEqualByComparingTo("50000");
        assertThat(result.mensualite()).isEqualByComparingTo("25000");
        assertThat(result.autresFrais()).isEqualByComparingTo("10000");
    }

    @Test
    void executer_leve_exception_si_classe_introuvable() {
        when(classeRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.executer(CLASSE_UUID))
                .isInstanceOf(SchoolNotFoundException.class);
    }

    @Test
    void executer_leve_exception_si_aucun_tarif_actif() {
        Classe classeSansTarif = Classe.reconstituer(ClasseId.of(CLASSE_UUID), "L1-INFO", "L1 Info",
                Cycle.LICENCE, Niveau.L1, FiliereId.of(UUID.randomUUID()), List.of());
        when(classeRepository.findById(any())).thenReturn(Optional.of(classeSansTarif));

        assertThatThrownBy(() -> service.executer(CLASSE_UUID))
                .isInstanceOf(SchoolNotFoundException.class)
                .hasMessageContaining("tarif actif");
    }

    @Test
    void executer_leve_exception_si_tarif_introuvable_en_base() {
        when(classeRepository.findById(any())).thenReturn(Optional.of(classeAvecTarifActif()));
        when(tarifRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.executer(CLASSE_UUID))
                .isInstanceOf(SchoolNotFoundException.class);
    }
}
