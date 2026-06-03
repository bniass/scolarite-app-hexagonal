package com.ecole221.anneeacademique.service.infrastructure.event.mapper;

import com.ecole221.anneeacademique.service.domain.event.AnneeAcademiqueCreeeEvent;
import com.ecole221.anneeacademique.service.domain.event.AnneeAcademiqueEvent;
import com.ecole221.anneeacademique.service.domain.model.MoisAcademique;
import com.ecole221.common.avro.CreateAnneeAcademiqueAvroModel;

import java.util.List;
import java.util.stream.Collectors;

public class AnneeMapper {

    public static CreateAnneeAcademiqueAvroModel toAvro(AnneeAcademiqueEvent event) {
        String moisJson = "[]";
        if (event instanceof AnneeAcademiqueCreeeEvent creeeEvent) {
            moisJson = serializerMois(creeeEvent.getMoisAcademiques());
        }
        return CreateAnneeAcademiqueAvroModel.newBuilder()
                .setCodeAnnee(event.getCode())
                .setEtatAnnee(event.getEtatAnnee())
                .setMoisAcademiques(moisJson)
                .build();
    }

    private static String serializerMois(List<MoisAcademique> mois) {
        if (mois == null || mois.isEmpty()) return "[]";
        return mois.stream()
                .map(m -> "{\"mois\":" + m.mois() + ",\"annee\":" + m.annee() + "}")
                .collect(Collectors.joining(",", "[", "]"));
    }
}
