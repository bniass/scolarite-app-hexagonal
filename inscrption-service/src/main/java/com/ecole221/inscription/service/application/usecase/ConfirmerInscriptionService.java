package com.ecole221.inscription.service.application.usecase;

import com.ecole221.inscription.service.application.port.in.ConfirmerInscriptionUseCase;
import com.ecole221.inscription.service.application.port.out.InscriptionRepository;
import com.ecole221.inscription.service.domain.exception.InscriptionNotFoundException;
import com.ecole221.inscription.service.domain.model.Inscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConfirmerInscriptionService implements ConfirmerInscriptionUseCase {

    private final InscriptionRepository inscriptionRepository;

    @Override
    @Transactional
    public void executer(UUID inscriptionId) {
        Inscription inscription = inscriptionRepository.trouverParId(inscriptionId)
                .orElseThrow(() -> new InscriptionNotFoundException(
                        "Inscription introuvable : " + inscriptionId));
        inscription.confirmer();
        inscriptionRepository.sauvegarder(inscription);
    }
}
