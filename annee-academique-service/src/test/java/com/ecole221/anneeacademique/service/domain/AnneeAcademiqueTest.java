package com.ecole221.anneeacademique.service.domain;

import com.ecole221.anneeacademique.service.domain.model.AnneeAcademique;
import com.ecole221.anneeacademique.service.domain.model.DatesAnnee;
import com.ecole221.anneeacademique.service.domain.event.AnneeAcademiqueCreeeEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class AnneeAcademiqueTest {
    @Test
    void creation_reussie_emet_evenement_creee_et_place_en_brouillon() {
        AnneeAcademique a = AnneeAcademique.creer(2025, datesValides());

        var events = a.pullDomainEvents();
        assertTrue(events.stream().anyMatch(e -> e instanceof AnneeAcademiqueCreeeEvent));
        assertEquals(9, a.getMoisAcademiques().size());

        // brouillon → publication autorisée
        assertDoesNotThrow(a::publier);

    }

    private DatesAnnee datesValides() {
        return new DatesAnnee(
                LocalDate.of(2025, 10, 1),
                LocalDate.of(2026, 6, 30),
                LocalDate.of(2025, 9, 1),
                LocalDate.of(2025, 10, 15)
        );
    }
}
