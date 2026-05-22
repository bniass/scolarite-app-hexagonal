package com.ecole221.etudiant.service.application.usecase;

import com.ecole221.common.event.DomainEvent;
import com.ecole221.etudiant.service.application.command.ModifierEtudiantCommand;
import com.ecole221.etudiant.service.application.port.in.ModifierEtudiantUseCase;
import com.ecole221.etudiant.service.application.port.out.EtudiantRepository;
import com.ecole221.etudiant.service.application.port.out.OutboxPort;
import com.ecole221.etudiant.service.domain.exception.EtudiantNotFoundException;
import com.ecole221.etudiant.service.domain.model.Etudiant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ModifierEtudiantService implements ModifierEtudiantUseCase {

    private final EtudiantRepository etudiantRepository;
    private final OutboxPort outboxPort;

    public ModifierEtudiantService(EtudiantRepository etudiantRepository, OutboxPort outboxPort) {
        this.etudiantRepository = etudiantRepository;
        this.outboxPort = outboxPort;
    }

    @Override
    public void executer(ModifierEtudiantCommand command) {
        Etudiant etudiant = etudiantRepository.trouverParMatricule(command.matricule())
                .orElseThrow(() -> new EtudiantNotFoundException(command.matricule()));

        etudiant.modifier(command.nom(), command.prenom(), command.dateNaissance());

        Etudiant saved = etudiantRepository.sauvegarder(etudiant);

        List<DomainEvent> events = saved.pullDomainEvents();
        events.forEach(outboxPort::sauvegarder);
    }
}
