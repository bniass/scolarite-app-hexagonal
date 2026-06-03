package com.ecole221.inscription.service.application.port.out;

import java.util.UUID;

public interface SchoolServicePort {
    TarifActifResult getTarifActif(UUID classeId);
}
