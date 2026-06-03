package com.ecole221.school.service.application.port.in;

import com.ecole221.school.service.application.command.AffecterTarifCommand;

public interface AffecterTarifUseCase {
    void executer(AffecterTarifCommand command);
}
