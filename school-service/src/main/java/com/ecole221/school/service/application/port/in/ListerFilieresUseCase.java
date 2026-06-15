package com.ecole221.school.service.application.port.in;

import com.ecole221.school.service.domain.model.Filiere;

import java.util.List;

public interface ListerFilieresUseCase {
    List<Filiere> executer();
}
