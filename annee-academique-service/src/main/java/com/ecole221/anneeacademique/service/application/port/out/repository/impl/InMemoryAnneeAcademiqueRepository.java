package com.ecole221.anneeacademique.service.application.port.out.repository.impl;

import com.ecole221.anneeacademique.service.application.port.out.repository.AnneeAcademiqueRepository;
import com.ecole221.anneeacademique.service.domain.model.AnneeAcademique;
import com.ecole221.anneeacademique.service.domain.model.Statut;
import com.ecole221.anneeacademique.service.domain.state.AnneeCloturee;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@Profile({"inmemory", "test"})
public class InMemoryAnneeAcademiqueRepository implements AnneeAcademiqueRepository {

    private final Map<String, AnneeAcademique> data = new HashMap<>();

    @Override
    public Optional<AnneeAcademique> findByCode(String code) {
        return Optional.ofNullable(data.get(code));
    }

    @Override
    public boolean existsByStatutNot(Statut statut) {
        return data.values().stream().anyMatch(
                anneeAcademique ->  !(anneeAcademique.getEtatAnnee() instanceof AnneeCloturee)
        );
    }


    @Override
    public List<AnneeAcademique> findAll() {
        return new ArrayList<>(data.values());
    }

    @Override
    public void save(AnneeAcademique anneeAcademique) {
        data.put(anneeAcademique.getId().getValue().getCodeAnnee(), anneeAcademique);
    }

}
