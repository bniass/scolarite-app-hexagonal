package com.ecole221.paiement.service.infrastructure;

import com.ecole221.common.avro.InscriptionAnnuleeAvroModel;
import com.ecole221.paiement.service.application.port.in.SupprimerDossierUseCase;
import com.ecole221.paiement.service.infrastructure.kafka.consumer.InscriptionAnnuleeKafkaConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InscriptionAnnuleeKafkaConsumerTest {

    @Mock private SupprimerDossierUseCase supprimerDossierUseCase;
    @InjectMocks private InscriptionAnnuleeKafkaConsumer consumer;

    @Test
    void receive_supprime_le_dossier_pour_chaque_message() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        var msg1 = InscriptionAnnuleeAvroModel.newBuilder().setInscriptionId(id1.toString()).build();
        var msg2 = InscriptionAnnuleeAvroModel.newBuilder().setInscriptionId(id2.toString()).build();

        consumer.receive(List.of(msg1, msg2), List.of("k1", "k2"), List.of(0, 0), List.of(0L, 1L));

        verify(supprimerDossierUseCase).supprimerParInscriptionId(id1);
        verify(supprimerDossierUseCase).supprimerParInscriptionId(id2);
    }

    @Test
    void receive_continue_si_une_suppression_echoue() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        var msg1 = InscriptionAnnuleeAvroModel.newBuilder().setInscriptionId(id1.toString()).build();
        var msg2 = InscriptionAnnuleeAvroModel.newBuilder().setInscriptionId(id2.toString()).build();

        doThrow(new RuntimeException("DB error")).when(supprimerDossierUseCase).supprimerParInscriptionId(id1);

        // ne doit pas propager l'exception — le second message est traité quand même
        consumer.receive(List.of(msg1, msg2), List.of("k1", "k2"), List.of(0, 0), List.of(0L, 1L));

        verify(supprimerDossierUseCase).supprimerParInscriptionId(id1);
        verify(supprimerDossierUseCase).supprimerParInscriptionId(id2);
    }
}
