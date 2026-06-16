package com.ecole221.inscription.service.infrastructure.web.controller;

import com.ecole221.inscription.service.application.command.ChangerClasseCommand;
import com.ecole221.inscription.service.application.command.CreerInscriptionCommand;
import com.ecole221.inscription.service.application.port.in.AnnulerInscriptionAdminUseCase;
import com.ecole221.inscription.service.application.port.in.ChangerClasseUseCase;
import com.ecole221.inscription.service.application.port.in.ConsulterInscriptionUseCase;
import com.ecole221.inscription.service.application.port.in.CreerInscriptionUseCase;
import com.ecole221.inscription.service.domain.model.Inscription;
import com.ecole221.inscription.service.infrastructure.web.dto.AnnulerInscriptionRequest;
import com.ecole221.inscription.service.infrastructure.web.dto.ChangerClasseRequest;
import com.ecole221.inscription.service.infrastructure.web.dto.CreerInscriptionRequest;
import com.ecole221.inscription.service.infrastructure.web.dto.InscriptionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/inscriptions")
@RequiredArgsConstructor
public class InscriptionController {

    private final CreerInscriptionUseCase creerUC;
    private final ConsulterInscriptionUseCase consulterUC;
    private final AnnulerInscriptionAdminUseCase annulerUC;
    private final ChangerClasseUseCase changerClasseUC;

    @PostMapping
    public ResponseEntity<InscriptionResponse> creer(@Valid @RequestBody CreerInscriptionRequest request) {
        String nom = null, prenom = null, email = null;
        java.time.LocalDate dateNaissance = null;
        if (request.nouvelEtudiant() != null) {
            nom           = request.nouvelEtudiant().nom();
            prenom        = request.nouvelEtudiant().prenom();
            dateNaissance = request.nouvelEtudiant().dateNaissance();
            email         = request.nouvelEtudiant().email();
        }

        CreerInscriptionCommand command = new CreerInscriptionCommand(
                request.etudiantId(),
                nom, prenom, dateNaissance, email,
                request.classeId(),
                request.codeAnnee()
        );

        Inscription inscription = creerUC.executer(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(InscriptionResponse.from(inscription));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InscriptionResponse> consulter(@PathVariable UUID id) {
        return ResponseEntity.ok(InscriptionResponse.from(consulterUC.parId(id)));
    }

    @PostMapping("/{id}/annuler")
    public ResponseEntity<Void> annuler(
            @PathVariable UUID id,
            @Valid @RequestBody AnnulerInscriptionRequest request) {
        annulerUC.executer(id, request.motif());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/changer-classe")
    public ResponseEntity<InscriptionResponse> changerClasse(
            @PathVariable UUID id,
            @Valid @RequestBody ChangerClasseRequest request) {
        Inscription inscription = changerClasseUC.executer(
                new ChangerClasseCommand(id, request.nouvelleClasseId()));
        return ResponseEntity.ok(InscriptionResponse.from(inscription));
    }
}
