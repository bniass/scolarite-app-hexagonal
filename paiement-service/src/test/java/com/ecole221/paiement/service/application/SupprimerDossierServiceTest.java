package com.ecole221.paiement.service.application;

import com.ecole221.paiement.service.application.port.out.DossierPaiementRepository;
import com.ecole221.paiement.service.application.usecase.SupprimerDossierService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupprimerDossierServiceTest {

    @Mock private DossierPaiementRepository repository;
    @InjectMocks private SupprimerDossierService service;

    @Test
    void supprimerParInscriptionId_delegue_au_repository() {
        UUID inscriptionId = UUID.randomUUID();

        service.supprimerParInscriptionId(inscriptionId);

        verify(repository, times(1)).supprimerParInscriptionId(inscriptionId);
    }

    @Test
    void supprimerParInscriptionId_ne_leve_pas_exception_si_dossier_inexistant() {
        UUID inscriptionId = UUID.randomUUID();
        doNothing().when(repository).supprimerParInscriptionId(any());

        // doit s'exécuter sans exception même si le dossier n'existe pas
        service.supprimerParInscriptionId(inscriptionId);

        verify(repository).supprimerParInscriptionId(inscriptionId);
    }
}
