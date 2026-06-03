package com.ecole221.etudiant.service.application;

import com.ecole221.etudiant.service.application.port.out.EtudiantRepository;
import com.ecole221.etudiant.service.application.usecase.RechercherEtudiantService;
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
class RechercherEtudiantServiceTest {

    @Mock private EtudiantRepository repository;
    @InjectMocks private RechercherEtudiantService service;

    private static final UUID ID = UUID.randomUUID();
    private static final String MATRICULE = "M00001-2425";

    private Etudiant etudiantExistant() {
        return Etudiant.reconstituer(
                new EtudiantId(ID), new Matricule(MATRICULE),
                "Diallo", "Mamadou", LocalDate.of(2000, 5, 15), "m@e.sn"
        );
    }

    @Test
    void parMatricule_retourne_letudiant() {
        when(repository.trouverParMatricule(MATRICULE)).thenReturn(Optional.of(etudiantExistant()));

        Etudiant result = service.parMatricule(MATRICULE);

        assertThat(result.getMatricule().getValeur()).isEqualTo(MATRICULE);
    }

    @Test
    void parMatricule_leve_exception_si_introuvable() {
        when(repository.trouverParMatricule(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.parMatricule("INCONNU"))
                .isInstanceOf(EtudiantNotFoundException.class)
                .hasMessageContaining("INCONNU");
    }

    @Test
    void parId_retourne_letudiant() {
        when(repository.trouverParId(ID)).thenReturn(Optional.of(etudiantExistant()));

        Etudiant result = service.parId(ID);

        assertThat(result.getId().getValue()).isEqualTo(ID);
    }

    @Test
    void parId_leve_exception_si_introuvable() {
        when(repository.trouverParId(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.parId(ID))
                .isInstanceOf(EtudiantNotFoundException.class)
                .hasMessageContaining(ID.toString());
    }
}
