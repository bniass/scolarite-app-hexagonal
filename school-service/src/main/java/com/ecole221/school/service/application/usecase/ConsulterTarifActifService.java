package com.ecole221.school.service.application.usecase;

import com.ecole221.school.service.application.port.in.ConsulterTarifActifUseCase;
import com.ecole221.school.service.application.port.out.repository.ClasseRepository;
import com.ecole221.school.service.application.port.out.repository.TarifRepository;
import com.ecole221.school.service.domain.exception.SchoolNotFoundException;
import com.ecole221.school.service.domain.model.Classe;
import com.ecole221.school.service.domain.model.ClasseId;
import com.ecole221.school.service.domain.model.ClasseTarif;
import com.ecole221.school.service.domain.model.Tarif;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ConsulterTarifActifService implements ConsulterTarifActifUseCase {

    private final ClasseRepository classeRepository;
    private final TarifRepository tarifRepository;

    public ConsulterTarifActifService(ClasseRepository classeRepository, TarifRepository tarifRepository) {
        this.classeRepository = classeRepository;
        this.tarifRepository = tarifRepository;
    }

    @Override
    public TarifActifResult executer(UUID classeId) {
        Classe classe = classeRepository.findById(ClasseId.of(classeId))
                .orElseThrow(() -> new SchoolNotFoundException("Classe introuvable : " + classeId));

        ClasseTarif classeTarif = classe.getTarifActif();
        if (classeTarif == null) {
            throw new SchoolNotFoundException("Aucun tarif actif pour la classe : " + classeId);
        }

        Tarif tarif = tarifRepository.findById(classeTarif.getTarifId())
                .orElseThrow(() -> new SchoolNotFoundException("Tarif introuvable : " + classeTarif.getTarifId().getValue()));

        return new TarifActifResult(
                classeId,
                classe.getCode(),
                classe.getNom(),
                classe.getCycle(),
                classe.getNiveau(),
                tarif.getId().getValue(),
                tarif.getFraisInscription(),
                tarif.getMensualite(),
                tarif.getAutresFrais()
        );
    }
}
