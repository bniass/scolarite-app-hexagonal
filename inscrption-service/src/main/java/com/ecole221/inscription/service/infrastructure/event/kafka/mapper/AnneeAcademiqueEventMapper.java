package com.ecole221.inscription.service.infrastructure.event.kafka.mapper;

import com.ecole221.common.avro.CreateAnneeAcademiqueAvroModel;
import com.ecole221.inscription.service.application.command.SynchroniserAnneeAcademiqueCommand;
import com.ecole221.inscription.service.domain.valueobject.CodeAnnee;
import com.ecole221.inscription.service.domain.valueobject.EtatAnnee;
import org.springframework.stereotype.Component;

@Component
public class AnneeAcademiqueEventMapper {

    public SynchroniserAnneeAcademiqueCommand toCommand(
            CreateAnneeAcademiqueAvroModel createAnneeAcademiqueAvroModel) {
        return new SynchroniserAnneeAcademiqueCommand(
                new CodeAnnee(createAnneeAcademiqueAvroModel.getCodeAnnee()),
                EtatAnnee.valueOf(createAnneeAcademiqueAvroModel.getEtatAnnee())
        );
    }
}
