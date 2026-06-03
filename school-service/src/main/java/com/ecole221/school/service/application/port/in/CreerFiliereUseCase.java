package com.ecole221.school.service.application.port.in;

import com.ecole221.school.service.application.command.CreerFiliereCommand;

import java.util.UUID;

public interface CreerFiliereUseCase {
    UUID executer(CreerFiliereCommand command);
}
