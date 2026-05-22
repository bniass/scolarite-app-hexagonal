package com.ecole221.etudiant.service.application.usecase;

import com.ecole221.common.event.DomainEvent;
import com.ecole221.etudiant.service.application.command.CreerEtudiantCommand;
import com.ecole221.etudiant.service.application.port.in.CreerEtudiantUseCase;
import com.ecole221.etudiant.service.application.port.out.EtudiantRepository;
import com.ecole221.etudiant.service.application.port.out.OutboxPort;
import com.ecole221.etudiant.service.domain.exception.EtudiantException;
import com.ecole221.etudiant.service.domain.model.Etudiant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CreerEtudiantService implements CreerEtudiantUseCase {

    private final EtudiantRepository etudiantRepository;
    private final OutboxPort outboxPort;

    public CreerEtudiantService(EtudiantRepository etudiantRepository, OutboxPort outboxPort) {
        this.etudiantRepository = etudiantRepository;
        this.outboxPort = outboxPort;
    }

    @Override
    public Etudiant executer(CreerEtudiantCommand command) {
        if (etudiantRepository.existeParMatricule(command.matricule())) {
            throw new EtudiantException("Un étudiant existe déjà avec le matricule : " + command.matricule());
        }

        Etudiant etudiant = Etudiant.creer(
                command.matricule(),
                command.nom(),
                command.prenom(),
                command.dateNaissance()
        );

        etudiantRepository.sauvegarder(etudiant);

        List<DomainEvent> events = etudiant.pullDomainEvents();
        events.forEach(outboxPort::sauvegarder);

        return etudiant;
    }
}
