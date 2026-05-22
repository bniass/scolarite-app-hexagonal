package com.ecole221.anneeacademique.service.infrastructure.event.mapper;

import com.ecole221.anneeacademique.service.domain.event.AnneeAcademiqueEvent;
import com.ecole221.common.avro.CreateAnneeAcademiqueAvroModel;

public class AnneeMapper {

    public static CreateAnneeAcademiqueAvroModel toAvro(AnneeAcademiqueEvent event) {
        return CreateAnneeAcademiqueAvroModel.newBuilder()
                .setCodeAnnee(event.getCode())
                .setEtatAnnee(event.getEtatAnnee())
                .build();
    }
}
