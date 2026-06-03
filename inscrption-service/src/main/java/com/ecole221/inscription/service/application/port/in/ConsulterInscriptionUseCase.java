package com.ecole221.inscription.service.application.port.in;

import com.ecole221.inscription.service.domain.model.Inscription;

import java.util.UUID;

public interface ConsulterInscriptionUseCase {
    Inscription parId(UUID id);
}
