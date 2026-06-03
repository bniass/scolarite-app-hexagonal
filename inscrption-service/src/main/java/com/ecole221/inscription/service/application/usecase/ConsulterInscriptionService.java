package com.ecole221.inscription.service.application.usecase;

import com.ecole221.inscription.service.application.port.in.ConsulterInscriptionUseCase;
import com.ecole221.inscription.service.application.port.out.InscriptionRepository;
import com.ecole221.inscription.service.domain.exception.InscriptionNotFoundException;
import com.ecole221.inscription.service.domain.model.Inscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsulterInscriptionService implements ConsulterInscriptionUseCase {

    private final InscriptionRepository inscriptionRepository;

    @Override
    @Transactional(readOnly = true)
    public Inscription parId(UUID id) {
        return inscriptionRepository.trouverParId(id)
                .orElseThrow(() -> new InscriptionNotFoundException("Inscription introuvable : " + id));
    }
}
