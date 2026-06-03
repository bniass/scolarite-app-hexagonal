package com.ecole221.etudiant.service.infrastructure.outbox.mapper;

import com.ecole221.common.avro.EtudiantCreeAvroModel;
import com.ecole221.common.avro.EtudiantModifieAvroModel;
import com.ecole221.etudiant.service.domain.event.EtudiantCreeEvent;
import com.ecole221.etudiant.service.domain.event.EtudiantModifieEvent;

public class EtudiantMapper {

    public static EtudiantCreeAvroModel toAvro(EtudiantCreeEvent event) {
        return EtudiantCreeAvroModel.newBuilder()
                .setEtudiantId(event.getEtudiantId().toString())
                .setMatricule(event.getMatricule())
                .setNom(event.getNom())
                .setPrenom(event.getPrenom())
                .setDateNaissance(event.getDateNaissance().toString())
                .setOccurredAt(event.occurredAt().toString())
                .setEmail(event.getEmail())
                .build();
    }

    public static EtudiantModifieAvroModel toAvro(EtudiantModifieEvent event) {
        return EtudiantModifieAvroModel.newBuilder()
                .setEtudiantId(event.getEtudiantId().toString())
                .setMatricule(event.getMatricule())
                .setNom(event.getNom())
                .setPrenom(event.getPrenom())
                .setDateNaissance(event.getDateNaissance().toString())
                .setOccurredAt(event.occurredAt().toString())
                .build();
    }
}
