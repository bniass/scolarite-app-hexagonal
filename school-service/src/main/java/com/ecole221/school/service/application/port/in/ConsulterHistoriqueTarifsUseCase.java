package com.ecole221.school.service.application.port.in;

import com.ecole221.school.service.application.usecase.HistoriqueTarifItem;

import java.util.List;
import java.util.UUID;

public interface ConsulterHistoriqueTarifsUseCase {
    List<HistoriqueTarifItem> executer(UUID classeId);
}
