package com.ecole221.paiement.service.application;

import com.ecole221.paiement.service.application.command.EffectuerVersementCommand;
import com.ecole221.paiement.service.application.port.out.DossierPaiementRepository;
import com.ecole221.paiement.service.application.usecase.EffectuerVersementService;
import com.ecole221.paiement.service.domain.exception.PaiementDomainException;
import com.ecole221.paiement.service.domain.model.*;
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
class EffectuerVersementServiceTest {

    @Mock private DossierPaiementRepository repository;
    @InjectMocks private EffectuerVersementService service;

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

    private UUID idLigneParType(TypeLigne type) {
        return dossier.getLignes().stream()
                .filter(l -> l.getType() == type)
                .findFirst().orElseThrow().getId();
    }

    @Test
    void executer_retourne_dossier_mis_a_jour() {
        EffectuerVersementCommand cmd = new EffectuerVersementCommand(
                inscriptionId, idLigneParType(TypeLigne.FRAIS_INSCRIPTION),
                new BigDecimal("50000"), LocalDate.now(), List.of(MoyenPaiement.comptant()));

        DossierPaiement result = service.executer(cmd);

        assertThat(result).isNotNull();
    }

    @Test
    void executer_marque_ligne_payee_si_montant_exact() {
        UUID ligneId = idLigneParType(TypeLigne.FRAIS_INSCRIPTION);
        EffectuerVersementCommand cmd = new EffectuerVersementCommand(
                inscriptionId, ligneId, new BigDecimal("50000"),
                LocalDate.now(), List.of(MoyenPaiement.comptant()));

        DossierPaiement result = service.executer(cmd);

        LignePaiement ligne = result.getLignes().stream()
                .filter(l -> l.getId().equals(ligneId)).findFirst().orElseThrow();
        assertThat(ligne.getStatut()).isEqualTo(StatutLigne.PAYE);
    }

    @Test
    void executer_leve_exception_si_dossier_introuvable() {
        when(repository.trouverParInscriptionId(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.executer(new EffectuerVersementCommand(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("50000"),
                LocalDate.now(), List.of(MoyenPaiement.comptant()))))
                .isInstanceOf(PaiementDomainException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    void executer_sauvegarde_le_dossier() {
        EffectuerVersementCommand cmd = new EffectuerVersementCommand(
                inscriptionId, idLigneParType(TypeLigne.FRAIS_INSCRIPTION),
                new BigDecimal("50000"), LocalDate.now(), List.of(MoyenPaiement.comptant()));

        service.executer(cmd);

        verify(repository).sauvegarder(any(DossierPaiement.class));
    }
}
