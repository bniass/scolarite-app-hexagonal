package com.ecole221.etudiant.service.application.usecase;

import com.ecole221.common.event.DomainEvent;
import com.ecole221.etudiant.service.application.command.CreerEtudiantCommand;
import com.ecole221.etudiant.service.application.port.in.CreerEtudiantUseCase;
import com.ecole221.etudiant.service.application.port.out.EtudiantRepository;
import com.ecole221.etudiant.service.application.port.out.OutboxPort;
import com.ecole221.etudiant.service.domain.exception.EtudiantException;
import com.ecole221.etudiant.service.domain.model.Etudiant;
import com.ecole221.etudiant.service.infrastructure.persistence.entity.MatriculeSequenceJpaEntity;
import com.ecole221.etudiant.service.infrastructure.persistence.repository.MatriculeSequenceJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CreerEtudiantService implements CreerEtudiantUseCase {

    private final EtudiantRepository etudiantRepository;
    private final OutboxPort outboxPort;
    private final MatriculeSequenceJpaRepository matriculeSequenceRepository;

    public CreerEtudiantService(EtudiantRepository etudiantRepository, OutboxPort outboxPort,
                                 MatriculeSequenceJpaRepository matriculeSequenceRepository) {
        this.etudiantRepository = etudiantRepository;
        this.outboxPort = outboxPort;
        this.matriculeSequenceRepository = matriculeSequenceRepository;
    }

    @Override
    public Etudiant executer(CreerEtudiantCommand command) {
        if (etudiantRepository.existeParEmail(command.email())) {
            throw new EtudiantException("Un étudiant existe déjà avec l'email : " + command.email());
        }

        String suffixe = Etudiant.suffixeAnnee(command.codeAnnee());
        // AUTO_INCREMENT garantit un ordre unique même sous concurrence totale
        long ordre = matriculeSequenceRepository.save(new MatriculeSequenceJpaEntity(suffixe)).getId();

        Etudiant etudiant = Etudiant.creer(
                command.nom(),
                command.prenom(),
                command.dateNaissance(),
                command.email(),
                ordre,
                command.codeAnnee()
        );

        etudiantRepository.sauvegarder(etudiant);

        List<DomainEvent> events = etudiant.pullDomainEvents();
        events.forEach(outboxPort::sauvegarder);

        return etudiant;
    }
}
