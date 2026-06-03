package com.ecole221.paiement.service.application;

import com.ecole221.paiement.service.application.port.out.DossierPaiementRepository;
import com.ecole221.paiement.service.application.usecase.ConsulterDossierService;
import com.ecole221.paiement.service.domain.exception.PaiementDomainException;
import com.ecole221.paiement.service.domain.model.*;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsulterDossierServiceTest {

    @Mock private DossierPaiementRepository repository;
    @InjectMocks private ConsulterDossierService service;

    private static final List<MoisAcademique> MOIS = List.of(
            new MoisAcademique(9, 2024), new MoisAcademique(10, 2024),
            new MoisAcademique(11, 2024), new MoisAcademique(12, 2024),
            new MoisAcademique(1, 2025), new MoisAcademique(2, 2025),
            new MoisAcademique(3, 2025), new MoisAcademique(4, 2025),
            new MoisAcademique(5, 2025)
    );

    @Test
    void parInscriptionId_retourne_le_dossier() {
        UUID inscriptionId = UUID.randomUUID();
        DossierPaiement dossier = DossierPaiement.initialiser(
                inscriptionId, UUID.randomUUID(), UUID.randomUUID(), "2024-2025",
                new BigDecimal("50000"), new BigDecimal("25000"), new BigDecimal("10000"), MOIS
        );
        when(repository.trouverParInscriptionId(inscriptionId)).thenReturn(Optional.of(dossier));

        DossierPaiement result = service.parInscriptionId(inscriptionId);

        assertThat(result).isNotNull();
        assertThat(result.getInscriptionId()).isEqualTo(inscriptionId);
    }

    @Test
    void parInscriptionId_leve_exception_si_dossier_introuvable() {
        UUID inscriptionId = UUID.randomUUID();
        when(repository.trouverParInscriptionId(inscriptionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.parInscriptionId(inscriptionId))
                .isInstanceOf(PaiementDomainException.class)
                .hasMessageContaining(inscriptionId.toString());
    }
}
