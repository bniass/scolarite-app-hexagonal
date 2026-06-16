package com.ecole221.paiement.service.application;

import com.ecole221.paiement.service.application.command.InitialiserDossierCommand;
import com.ecole221.paiement.service.application.port.in.InitialiserDossierUseCase;
import com.ecole221.paiement.service.application.port.out.DossierPaiementRepository;
import com.ecole221.paiement.service.application.usecase.TransfererDossierService;
import com.ecole221.paiement.service.domain.model.DossierPaiement;
import com.ecole221.paiement.service.domain.model.MoisAcademique;
import com.ecole221.paiement.service.domain.model.MoyenPaiement;
import com.ecole221.paiement.service.domain.model.Versement;
import com.ecole221.paiement.service.domain.valueobject.StatutDossier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransfererDossierServiceTest {

    @Mock private DossierPaiementRepository repository;
    @Mock private InitialiserDossierUseCase initialiserDossierUseCase;
    @InjectMocks private TransfererDossierService service;

    private static final List<MoisAcademique> MOIS = List.of(
            new MoisAcademique(10, 2025), new MoisAcademique(11, 2025)
    );

    private InitialiserDossierCommand command(UUID inscriptionId, UUID classeId) {
        return new InitialiserDossierCommand(inscriptionId, UUID.randomUUID(), classeId,
                "2025-2026", new BigDecimal("50000"), new BigDecimal("30000"),
                new BigDecimal("10000"), MOIS);
    }

    @Test
    void executer_redistribue_le_montant_paye_sur_le_nouveau_dossier() {
        UUID inscriptionId = UUID.randomUUID();
        UUID nouvelleClasse = UUID.randomUUID();

        // Ancien dossier avec 80 000 FCFA déjà payés
        DossierPaiement ancienDossier = DossierPaiement.initialiser(
                inscriptionId, UUID.randomUUID(), UUID.randomUUID(), "2025-2026",
                new BigDecimal("50000"), new BigDecimal("30000"), new BigDecimal("10000"), MOIS);
        ancienDossier.distribuerVersement(new BigDecimal("90000"), LocalDate.now(), MoyenPaiement.comptant());

        BigDecimal totalPaye = ancienDossier.getLignes().stream()
                .map(l -> l.getMontantPaye())
                .reduce(BigDecimal.ZERO, BigDecimal::add); // 90 000

        // Nouveau dossier vide
        DossierPaiement nouveauDossier = DossierPaiement.initialiser(
                inscriptionId, UUID.randomUUID(), nouvelleClasse, "2025-2026",
                new BigDecimal("50000"), new BigDecimal("30000"), new BigDecimal("10000"), MOIS);

        when(repository.trouverParInscriptionId(inscriptionId)).thenReturn(Optional.of(ancienDossier));
        when(initialiserDossierUseCase.executer(any())).thenReturn(nouveauDossier);
        when(repository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));

        service.executer(command(inscriptionId, nouvelleClasse));

        // L'ancien dossier est supprimé
        verify(repository).supprimerParInscriptionId(inscriptionId);

        // Le nouveau dossier est sauvegardé avec le montant redistribué
        ArgumentCaptor<DossierPaiement> captor = ArgumentCaptor.forClass(DossierPaiement.class);
        verify(repository).sauvegarder(captor.capture());

        DossierPaiement sauvegarde = captor.getValue();
        assertThat(sauvegarde.getStatut()).isEqualTo(StatutDossier.ACTIF);
        BigDecimal totalPayeNouveau = sauvegarde.getLignes().stream()
                .map(l -> l.getMontantPaye())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalPayeNouveau).isEqualByComparingTo(totalPaye);
    }

    @Test
    void executer_cree_dossier_vide_si_aucun_paiement_precedent() {
        UUID inscriptionId = UUID.randomUUID();

        when(repository.trouverParInscriptionId(inscriptionId)).thenReturn(Optional.empty());

        DossierPaiement nouveauDossier = DossierPaiement.initialiser(
                inscriptionId, UUID.randomUUID(), UUID.randomUUID(), "2025-2026",
                new BigDecimal("50000"), new BigDecimal("30000"), new BigDecimal("10000"), MOIS);
        when(initialiserDossierUseCase.executer(any())).thenReturn(nouveauDossier);

        service.executer(command(inscriptionId, UUID.randomUUID()));

        verify(repository).supprimerParInscriptionId(inscriptionId);
        // pas de sauvegarder supplémentaire — totalPayé = 0
        verify(repository, never()).sauvegarder(any());
        assertThat(nouveauDossier.getStatut()).isEqualTo(StatutDossier.INITIALISE);
    }
}
