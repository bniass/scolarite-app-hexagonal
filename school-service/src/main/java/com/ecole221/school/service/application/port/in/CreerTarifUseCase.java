package com.ecole221.school.service.application.port.in;

import com.ecole221.school.service.application.command.CreerTarifCommand;

import java.util.UUID;

public interface CreerTarifUseCase {
    UUID executer(CreerTarifCommand command);
}
