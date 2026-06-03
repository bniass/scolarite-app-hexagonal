package com.ecole221.school.service.application.port.in;

import com.ecole221.school.service.application.usecase.TarifActifResult;

import java.util.UUID;

public interface ConsulterTarifActifUseCase {
    TarifActifResult executer(UUID classeId);
}
