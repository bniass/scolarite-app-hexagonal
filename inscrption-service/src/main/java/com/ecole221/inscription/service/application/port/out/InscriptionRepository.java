package com.ecole221.inscription.service.application.port.out;

import com.ecole221.inscription.service.domain.model.Inscription;

import java.util.Optional;
import java.util.UUID;

public interface InscriptionRepository {
    Inscription sauvegarder(Inscription inscription);
    Optional<Inscription> trouverParId(UUID id);
    void supprimer(UUID id);
}
