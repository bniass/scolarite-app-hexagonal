package com.ecole221.school.service.application.port.in;

import com.ecole221.school.service.application.command.CreerClasseCommand;

import java.util.UUID;

public interface CreerClasseUseCase {
    UUID executer(CreerClasseCommand command);
}
