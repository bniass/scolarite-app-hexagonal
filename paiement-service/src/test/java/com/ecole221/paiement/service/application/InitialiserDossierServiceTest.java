package com.ecole221.paiement.service.application;

import com.ecole221.paiement.service.application.command.InitialiserDossierCommand;
import com.ecole221.paiement.service.application.port.out.DossierPaiementRepository;
import com.ecole221.paiement.service.application.port.out.PaiementOutboxPort;
import com.ecole221.paiement.service.application.usecase.InitialiserDossierService;
import com.ecole221.paiement.service.domain.exception.PaiementDomainException;
import com.ecole221.paiement.service.domain.model.DossierPaiement;
import com.ecole221.paiement.service.domain.model.MoisAcademique;
import com.ecole221.paiement.service.domain.valueobject.StatutDossier;
import com.ecole221.paiement.service.domain.valueobject.StatutLigne;
import com.ecole221.paiement.service.domain.valueobject.TypeLigne;
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
    @Mock private PaiementOutboxPort outboxPort;
    @InjectMocks private InitialiserDossierService service;

    // fraisInscription=50000, autresFrais=10000, mensualite=25000
    // Minimum = 85000 | Total = 285000
    private static final BigDecimal FRAIS        = new BigDecimal("50000");
    private static final BigDecimal MENSUALITE   = new BigDecimal("25000");
    private static final BigDecimal AUTRES_FRAIS = new BigDecimal("10000");
    private static final BigDecimal MINIMUM      = new BigDecimal("85000");

    private static final List<MoisAcademique> MOIS = List.of(
            new MoisAcademique(9, 2024), new MoisAcademique(10, 2024),
            new MoisAcademique(11, 2024), new MoisAcademique(12, 2024),
            new MoisAcademique(1, 2025), new MoisAcademique(2, 2025),
            new MoisAcademique(3, 2025), new MoisAcademique(4, 2025),
            new MoisAcademique(5, 2025)
    );

    private InitialiserDossierCommand command(BigDecimal montant) {
        return new InitialiserDossierCommand(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "2024-2025", FRAIS, MENSUALITE, AUTRES_FRAIS, MOIS,
                montant, "COMPTANT", "", "", "", ""
        );
    }

    @Test
    void executer_sauvegarde_le_dossier() {
        when(repository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));

        service.executer(command(MINIMUM));

        verify(repository).sauvegarder(any(DossierPaiement.class));
    }

    @Test
    void executer_publie_event_outbox() {
        when(repository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));

        service.executer(command(MINIMUM));

        verify(outboxPort).sauvegarder(any());
    }

    @Test
    void executer_versement_minimum_couvre_frais_autres_et_mois1() {
        when(repository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));

        DossierPaiement result = service.executer(command(MINIMUM));

        assertThat(result.getLignes().stream()
                .filter(l -> l.getType() == TypeLigne.FRAIS_INSCRIPTION)
                .findFirst().orElseThrow().getStatut()).isEqualTo(StatutLigne.PAYE);
        assertThat(result.getLignes().stream()
                .filter(l -> l.getType() == TypeLigne.AUTRES_FRAIS)
                .findFirst().orElseThrow().getStatut()).isEqualTo(StatutLigne.PAYE);
        assertThat(result.getStatut()).isEqualTo(StatutDossier.ACTIF);
    }

    @Test
    void executer_versement_inferieur_au_minimum_leve_exception() {
        // 80000 < 85000 minimum
        assertThatThrownBy(() -> service.executer(command(new BigDecimal("80000"))))
                .isInstanceOf(PaiementDomainException.class)
                .hasMessageContaining("minimum");
    }

    @Test
    void executer_versement_depasse_total_leve_exception() {
        // Total = 285000, verser 300000 → interdit
        assertThatThrownBy(() -> service.executer(command(new BigDecimal("300000"))))
                .isInstanceOf(PaiementDomainException.class)
                .hasMessageContaining("dépasse");
    }

    @Test
    void executer_versement_avec_surplus_cascade_sur_mensualites() {
        when(repository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));

        // 85000 + 3*25000 = 160000 → frais + autres + 4 mois
        DossierPaiement result = service.executer(command(new BigDecimal("160000")));

        long nbMoisPayees = result.getLignes().stream()
                .filter(l -> l.getType() == TypeLigne.MENSUALITE && l.estPayee())
                .count();
        assertThat(nbMoisPayees).isEqualTo(4);
    }

    @Test
    void executer_versement_avec_avance_partielle_sur_mois() {
        when(repository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));

        // 85000 + 10000 = 95000 → frais + autres + mois1 + 10000 sur mois2
        DossierPaiement result = service.executer(command(new BigDecimal("95000")));

        var mois2 = result.getLignes().stream()
                .filter(l -> l.getType() == TypeLigne.MENSUALITE && l.getOrdreReglement() == 2)
                .findFirst().orElseThrow();
        assertThat(mois2.getStatut()).isEqualTo(StatutLigne.APAYER);
        assertThat(mois2.getMontantRestant()).isEqualByComparingTo("15000");
    }

    @Test
    void executer_mobile_money_accepte() {
        when(repository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));

        InitialiserDossierCommand cmd = new InitialiserDossierCommand(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "2024-2025", FRAIS, MENSUALITE, AUTRES_FRAIS, MOIS,
                MINIMUM, "MOBILE_MONEY", "Orange", "OM-123456", "", ""
        );

        assertThatCode(() -> service.executer(cmd)).doesNotThrowAnyException();
    }

    @Test
    void executer_banque_accepte() {
        when(repository.sauvegarder(any())).thenAnswer(inv -> inv.getArgument(0));

        InitialiserDossierCommand cmd = new InitialiserDossierCommand(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "2024-2025", FRAIS, MENSUALITE, AUTRES_FRAIS, MOIS,
                MINIMUM, "BANQUE", "", "", "SGBS", "VIR-001"
        );

        assertThatCode(() -> service.executer(cmd)).doesNotThrowAnyException();
    }
}
