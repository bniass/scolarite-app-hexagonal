package com.ecole221.paiement.service.application;

import com.ecole221.paiement.service.application.command.InitialiserDossierCommand;
import com.ecole221.paiement.service.application.port.out.DossierPaiementRepository;
import com.ecole221.paiement.service.application.usecase.InitialiserDossierService;
import com.ecole221.paiement.service.domain.model.DossierPaiement;
import com.ecole221.paiement.service.domain.model.MoisAcademique;
import com.ecole221.paiement.service.domain.valueobject.StatutDossier;
import com.ecole221.paiement.service.domain.valueobject.StatutLigne;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InitialiserDossierServiceTest {

    @Mock private DossierPaiementRepository repository;
    @InjectMocks private InitialiserDossierService service;

    private static final List<MoisAcademique> MOIS = List.of(
            new MoisAcademique(9, 2024), new MoisAcademique(10, 2024),
            new MoisAcademique(11, 2024), new MoisAcademique(12, 2024),
            new MoisAcademique(1, 2025), new MoisAcademique(2, 2025),
            new MoisAcademique(3, 2025), new MoisAcademique(4, 2025),
            new MoisAcademique(5, 2025)
    );

    private InitialiserDossierCommand command() {
        return new InitialiserDossierCommand(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "2024-2025",
                new BigDecimal("50000"),
                new BigDecimal("25000"),
                new BigDecimal("10000"),
                MOIS
        );
    }

    @Test
    void executer_sauvegarde_le_dossier() {
        when(repository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));

        service.executer(command());

        verify(repository).sauvegarder(any(DossierPaiement.class));
    }

    @Test
    void executer_ne_publie_pas_event_confirmation_a_linit() {
        // L'inscription n'est confirmée qu'après le premier versement, pas à l'initialisation du dossier
        when(repository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));

        DossierPaiement result = service.executer(command());

        assertThat(result.pullDomainEvents()).isEmpty();
    }

    @Test
    void executer_dossier_est_INITIALISE() {
        when(repository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));

        DossierPaiement result = service.executer(command());

        assertThat(result.getStatut()).isEqualTo(StatutDossier.INITIALISE);
    }

    @Test
    void executer_toutes_les_lignes_sont_APAYER() {
        when(repository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));

        DossierPaiement result = service.executer(command());

        assertThat(result.getLignes())
                .isNotEmpty()
                .allMatch(l -> l.getStatut() == StatutLigne.APAYER);
    }

    @Test
    void executer_genere_11_lignes() {
        // 1 frais inscription + 1 autres frais + 9 mensualités
        when(repository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));

        DossierPaiement result = service.executer(command());

        assertThat(result.getLignes()).hasSize(11);
    }
}
