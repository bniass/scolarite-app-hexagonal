package com.ecole221.inscription.service.infrastructure.event.kafka.consumer;

import com.ecole221.common.avro.CreateAnneeAcademiqueAvroModel;
import com.ecole221.inscription.service.application.command.SynchroniserAnneeAcademiqueCommand;
import com.ecole221.inscription.service.application.port.in.SynchroniserAnneeAcademiqueUseCase;
import com.ecole221.inscription.service.infrastructure.event.kafka.mapper.AnneeAcademiqueEventMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class AnneeAcademiqueEventConsumer {

    private final SynchroniserAnneeAcademiqueUseCase useCase;
    private final AnneeAcademiqueEventMapper mapper;

    public AnneeAcademiqueEventConsumer(
            SynchroniserAnneeAcademiqueUseCase useCase,
            AnneeAcademiqueEventMapper mapper
    ) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @KafkaListener(
            topics = "${kafka-topics.anneeacademique-topic-request-name}",
            groupId = "default-group"
    )
    public void consume(@Payload CreateAnneeAcademiqueAvroModel message,
                        @Header(KafkaHeaders.RECEIVED_KEY) String key,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset) {

        SynchroniserAnneeAcademiqueCommand command = mapper.toCommand(message);

        useCase.synchroniser(command);
    }
}
