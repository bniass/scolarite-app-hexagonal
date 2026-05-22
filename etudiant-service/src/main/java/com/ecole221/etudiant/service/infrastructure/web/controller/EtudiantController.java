package com.ecole221.etudiant.service.infrastructure.web.controller;

import com.ecole221.etudiant.service.application.command.CreerEtudiantCommand;
import com.ecole221.etudiant.service.application.command.ModifierEtudiantCommand;
import com.ecole221.etudiant.service.application.port.in.CreerEtudiantUseCase;
import com.ecole221.etudiant.service.application.port.in.ModifierEtudiantUseCase;
import com.ecole221.etudiant.service.application.port.in.RechercherEtudiantUseCase;
import com.ecole221.etudiant.service.domain.model.Etudiant;
import com.ecole221.etudiant.service.infrastructure.web.dto.CreerEtudiantRequest;
import com.ecole221.etudiant.service.infrastructure.web.dto.EtudiantResponse;
import com.ecole221.etudiant.service.infrastructure.web.dto.ModifierEtudiantRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/etudiants")
public class EtudiantController {

    private final CreerEtudiantUseCase creerEtudiantUseCase;
    private final ModifierEtudiantUseCase modifierEtudiantUseCase;
    private final RechercherEtudiantUseCase rechercherEtudiantUseCase;

    public EtudiantController(CreerEtudiantUseCase creerEtudiantUseCase,
                               ModifierEtudiantUseCase modifierEtudiantUseCase,
                               RechercherEtudiantUseCase rechercherEtudiantUseCase) {
        this.creerEtudiantUseCase = creerEtudiantUseCase;
        this.modifierEtudiantUseCase = modifierEtudiantUseCase;
        this.rechercherEtudiantUseCase = rechercherEtudiantUseCase;
    }

    @PostMapping
    public ResponseEntity<EtudiantResponse> creer(@Valid @RequestBody CreerEtudiantRequest request) {
        Etudiant etudiant = creerEtudiantUseCase.executer(new CreerEtudiantCommand(
                request.getMatricule(),
                request.getNom(),
                request.getPrenom(),
                request.getDateNaissance()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(etudiant));
    }

    @PutMapping("/{matricule}")
    public ResponseEntity<Void> modifier(@PathVariable String matricule,
                                          @Valid @RequestBody ModifierEtudiantRequest request) {
        modifierEtudiantUseCase.executer(new ModifierEtudiantCommand(
                matricule,
                request.getNom(),
                request.getPrenom(),
                request.getDateNaissance()
        ));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{matricule}")
    public ResponseEntity<EtudiantResponse> rechercher(@PathVariable String matricule) {
        Etudiant etudiant = rechercherEtudiantUseCase.parMatricule(matricule);
        return ResponseEntity.ok(toResponse(etudiant));
    }

    private EtudiantResponse toResponse(Etudiant etudiant) {
        return new EtudiantResponse(
                etudiant.getId().getValue(),
                etudiant.getMatricule().getValeur(),
                etudiant.getNom(),
                etudiant.getPrenom(),
                etudiant.getDateNaissance()
        );
    }
}
