package com.ecole221.paiement.service.application;

import com.ecole221.paiement.service.application.command.DistribuerVersementCommand;
import com.ecole221.paiement.service.application.port.out.DossierPaiementRepository;
import com.ecole221.paiement.service.application.usecase.DistribuerVersementService;
import com.ecole221.paiement.service.domain.exception.PaiementDomainException;
import com.ecole221.paiement.service.domain.model.*;
import com.ecole221.paiement.service.domain.valueobject.StatutDossier;
import com.ecole221.paiement.service.domain.valueobject.StatutLigne;
import com.ecole221.paiement.service.domain.valueobject.TypeLigne;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DistribuerVersementServiceTest {

    @Mock private DossierPaiementRepository repository;
    @InjectMocks private DistribuerVersementService service;

    private static final List<MoisAcademique> MOIS = List.of(
            new MoisAcademique(9, 2024), new MoisAcademique(10, 2024),
            new MoisAcademique(11, 2024), new MoisAcademique(12, 2024),
            new MoisAcademique(1, 2025), new MoisAcademique(2, 2025),
            new MoisAcademique(3, 2025), new MoisAcademique(4, 2025),
            new MoisAcademique(5, 2025)
    );

    private DossierPaiement dossier;
    private UUID inscriptionId;

    @BeforeEach
    void setUp() {
        inscriptionId = UUID.randomUUID();
        dossier = DossierPaiement.initialiser(
                inscriptionId, UUID.randomUUID(), UUID.randomUUID(), "2024-2025",
                new BigDecimal("50000"), new BigDecimal("25000"), new BigDecimal("10000"), MOIS
        );
        when(repository.trouverParInscriptionId(inscriptionId)).thenReturn(Optional.of(dossier));
        when(repository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private DistribuerVersementCommand commandAvec(String montant) {
        return new DistribuerVersementCommand(
                inscriptionId, new BigDecimal(montant),
                LocalDate.now(), List.of(MoyenPaiement.comptant()));
    }

    @Test
    void executer_distribue_sur_frais_inscription_en_premier() {
        DossierPaiement result = service.executer(commandAvec("50000"));

        LignePaiement frais = result.getLignes().stream()
                .filter(l -> l.getType() == TypeLigne.FRAIS_INSCRIPTION)
                .findFirst().orElseThrow();
        assertThat(frais.getStatut()).isEqualTo(StatutLigne.PAYE);
    }

    @Test
    void executer_cascade_sur_plusieurs_lignes() {
        // 50000 frais + 10000 autres = 60000
        DossierPaiement result = service.executer(commandAvec("60000"));

        assertThat(result.getLignes().stream()
                .filter(l -> l.getType() == TypeLigne.FRAIS_INSCRIPTION)
                .findFirst().orElseThrow().getStatut()).isEqualTo(StatutLigne.PAYE);
        assertThat(result.getLignes().stream()
                .filter(l -> l.getType() == TypeLigne.AUTRES_FRAIS)
                .findFirst().orElseThrow().getStatut()).isEqualTo(StatutLigne.PAYE);
    }

    @Test
    void executer_sauvegarde_le_dossier() {
        service.executer(commandAvec("50000"));
        verify(repository).sauvegarder(any(DossierPaiement.class));
    }

    @Test
    void executer_leve_exception_si_dossier_introuvable() {
        when(repository.trouverParInscriptionId(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.executer(new DistribuerVersementCommand(
                UUID.randomUUID(), new BigDecimal("50000"),
                LocalDate.now(), List.of(MoyenPaiement.comptant()))))
                .isInstanceOf(PaiementDomainException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    void executer_passe_statut_dossier_a_ACTIF_apres_premier_versement() {
        DossierPaiement result = service.executer(commandAvec("50000"));
        assertThat(result.getStatut()).isEqualTo(StatutDossier.ACTIF);
    }
}
