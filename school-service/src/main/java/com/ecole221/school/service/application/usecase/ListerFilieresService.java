package com.ecole221.school.service.application.usecase;

import com.ecole221.school.service.application.port.in.ListerFilieresUseCase;
import com.ecole221.school.service.application.port.out.repository.FiliereRepository;
import com.ecole221.school.service.domain.model.Filiere;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ListerFilieresService implements ListerFilieresUseCase {

    private final FiliereRepository filiereRepository;

    public ListerFilieresService(FiliereRepository filiereRepository) {
        this.filiereRepository = filiereRepository;
    }

    @Override
    @Cacheable("filieres")
    public List<Filiere> executer() {
        return filiereRepository.findAll();
    }

    @CacheEvict(value = "filieres", allEntries = true)
    public void invaliderCache() {}
}
