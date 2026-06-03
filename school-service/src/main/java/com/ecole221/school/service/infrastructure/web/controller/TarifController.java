package com.ecole221.school.service.infrastructure.web.controller;

import com.ecole221.school.service.application.command.CreerTarifCommand;
import com.ecole221.school.service.application.port.in.CreerTarifUseCase;
import com.ecole221.school.service.infrastructure.web.dto.CreerTarifRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@Tag(name = "Tarif", description = "Gestion des tarifs")
@RequestMapping("/api/tarifs")
public class TarifController {

    private final CreerTarifUseCase creerUC;

    public TarifController(CreerTarifUseCase creerUC) {
        this.creerUC = creerUC;
    }

    @PostMapping
    public ResponseEntity<Void> creer(@Valid @RequestBody CreerTarifRequest request) {
        UUID id = creerUC.executer(new CreerTarifCommand(
                request.fraisInscription(),
                request.mensualite(),
                request.autresFrais()
        ));
        return ResponseEntity.created(URI.create("/api/tarifs/" + id)).build();
    }
}
