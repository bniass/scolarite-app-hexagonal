package com.ecole221.school.service.infrastructure.web.controller;

import com.ecole221.school.service.application.command.CreerFiliereCommand;
import com.ecole221.school.service.application.port.in.CreerFiliereUseCase;
import com.ecole221.school.service.infrastructure.web.dto.CreerFiliereRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@Tag(name = "Filiere", description = "Gestion des filières")
@RequestMapping("/api/filieres")
public class FiliereController {

    private final CreerFiliereUseCase creerUC;

    public FiliereController(CreerFiliereUseCase creerUC) {
        this.creerUC = creerUC;
    }

    @PostMapping
    public ResponseEntity<Void> creer(@Valid @RequestBody CreerFiliereRequest request) {
        UUID id = creerUC.executer(new CreerFiliereCommand(request.code(), request.nom()));
        return ResponseEntity.created(URI.create("/api/filieres/" + id)).build();
    }
}
