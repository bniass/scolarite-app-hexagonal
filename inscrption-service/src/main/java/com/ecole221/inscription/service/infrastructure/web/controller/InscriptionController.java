package com.ecole221.inscription.service.infrastructure.web.controller;

import com.ecole221.inscription.service.application.command.CreerInscriptionCommand;
import com.ecole221.inscription.service.application.port.in.ConsulterInscriptionUseCase;
import com.ecole221.inscription.service.application.port.in.CreerInscriptionUseCase;
import com.ecole221.inscription.service.domain.model.Inscription;
import com.ecole221.inscription.service.infrastructure.web.dto.CreerInscriptionRequest;
import com.ecole221.inscription.service.infrastructure.web.dto.InscriptionResponse;
import com.ecole221.inscription.service.infrastructure.web.dto.MoyenPaiementRequest;
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

    @PostMapping
    public ResponseEntity<InscriptionResponse> creer(@Valid @RequestBody CreerInscriptionRequest request) {
        CreerInscriptionCommand command = toCommand(request);
        Inscription inscription = creerUC.executer(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(InscriptionResponse.from(inscription));
    }

    private CreerInscriptionCommand toCommand(CreerInscriptionRequest request) {
        String typePaiement;
        String operateur = "";
        String referencePaiement = "";
        String nomBanque = "";
        String numeroTransaction = "";

        switch (request.moyenPaiement()) {
            case MoyenPaiementRequest.MobileMoney mm -> {
                typePaiement = "MOBILE_MONEY";
                operateur = mm.operateur() != null ? mm.operateur() : "";
                referencePaiement = mm.referencePaiement() != null ? mm.referencePaiement() : "";
            }
            case MoyenPaiementRequest.Banque b -> {
                typePaiement = "BANQUE";
                nomBanque = b.nomBanque() != null ? b.nomBanque() : "";
                numeroTransaction = b.numeroTransaction() != null ? b.numeroTransaction() : "";
            }
            case MoyenPaiementRequest.Comptant ignored -> typePaiement = "COMPTANT";
        }

        String nom = null, prenom = null, email = null;
        java.time.LocalDate dateNaissance = null;
        if (request.nouvelEtudiant() != null) {
            nom           = request.nouvelEtudiant().nom();
            prenom        = request.nouvelEtudiant().prenom();
            dateNaissance = request.nouvelEtudiant().dateNaissance();
            email         = request.nouvelEtudiant().email();
        }

        return new CreerInscriptionCommand(
                request.etudiantId(),
                nom, prenom, dateNaissance, email,
                request.classeId(), request.codeAnnee(),
                request.montant(), typePaiement,
                operateur, referencePaiement, nomBanque, numeroTransaction);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InscriptionResponse> consulter(@PathVariable UUID id) {
        return ResponseEntity.ok(InscriptionResponse.from(consulterUC.parId(id)));
    }
}
