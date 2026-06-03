package com.ecole221.etudiant.service.application;

import com.ecole221.etudiant.service.application.port.out.EtudiantRepository;
import com.ecole221.etudiant.service.application.usecase.SupprimerEtudiantService;
import com.ecole221.etudiant.service.domain.exception.EtudiantNotFoundException;
import com.ecole221.etudiant.service.domain.model.Etudiant;
import com.ecole221.etudiant.service.domain.valueobject.EtudiantId;
import com.ecole221.etudiant.service.domain.valueobject.Matricule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupprimerEtudiantServiceTest {

    @Mock private EtudiantRepository repository;
    @InjectMocks private SupprimerEtudiantService service;

    private static final UUID ID = UUID.randomUUID();

    private Etudiant etudiantExistant() {
        return Etudiant.reconstituer(
                new EtudiantId(ID), new Matricule("M00001-2425"),
                "Diallo", "Mamadou", LocalDate.of(2000, 5, 15), "m@e.sn"
        );
    }

    @Test
    void executer_supprime_etudiant_existant() {
        when(repository.trouverParId(ID)).thenReturn(Optional.of(etudiantExistant()));

        service.executer(ID);

        verify(repository).supprimerParId(ID);
    }

    @Test
    void executer_leve_exception_si_etudiant_introuvable() {
        when(repository.trouverParId(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.executer(ID))
                .isInstanceOf(EtudiantNotFoundException.class)
                .hasMessageContaining(ID.toString());
    }
}
