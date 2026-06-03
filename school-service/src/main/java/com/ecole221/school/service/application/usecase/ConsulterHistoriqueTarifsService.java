package com.ecole221.school.service.application.usecase;

import com.ecole221.school.service.application.port.in.ConsulterHistoriqueTarifsUseCase;
import com.ecole221.school.service.application.port.out.repository.ClasseRepository;
import com.ecole221.school.service.application.port.out.repository.TarifRepository;
import com.ecole221.school.service.domain.exception.SchoolNotFoundException;
import com.ecole221.school.service.domain.model.Classe;
import com.ecole221.school.service.domain.model.ClasseId;
import com.ecole221.school.service.domain.model.ClasseTarif;
import com.ecole221.school.service.domain.model.Tarif;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ConsulterHistoriqueTarifsService implements ConsulterHistoriqueTarifsUseCase {

    private final ClasseRepository classeRepository;
    private final TarifRepository tarifRepository;

    public ConsulterHistoriqueTarifsService(ClasseRepository classeRepository,
                                             TarifRepository tarifRepository) {
        this.classeRepository = classeRepository;
        this.tarifRepository = tarifRepository;
    }

    @Override
    public List<HistoriqueTarifItem> executer(UUID classeId) {
        Classe classe = classeRepository.findById(ClasseId.of(classeId))
                .orElseThrow(() -> new SchoolNotFoundException("Classe introuvable : " + classeId));

        return classe.getHistoriqueTarifs().stream()
                .map(ct -> toItem(ct))
                .toList();
    }

    private HistoriqueTarifItem toItem(ClasseTarif ct) {
        Tarif tarif = tarifRepository.findById(ct.getTarifId())
                .orElseThrow(() -> new SchoolNotFoundException("Tarif introuvable : " + ct.getTarifId().getValue()));
        return new HistoriqueTarifItem(
                tarif.getId().getValue(),
                tarif.getFraisInscription(),
                tarif.getMensualite(),
                tarif.getAutresFrais(),
                ct.getDateActivation(),
                ct.getDateDesactivation(),
                ct.isActif()
        );
    }
}
