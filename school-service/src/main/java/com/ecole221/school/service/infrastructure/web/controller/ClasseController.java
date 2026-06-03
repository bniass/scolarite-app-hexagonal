package com.ecole221.school.service.infrastructure.web.controller;

import com.ecole221.school.service.application.command.AffecterTarifCommand;
import com.ecole221.school.service.application.command.CreerClasseCommand;
import com.ecole221.school.service.application.port.in.AffecterTarifUseCase;
import com.ecole221.school.service.application.port.in.ConsulterHistoriqueTarifsUseCase;
import com.ecole221.school.service.application.port.in.ConsulterTarifActifUseCase;
import com.ecole221.school.service.application.port.in.CreerClasseUseCase;
import com.ecole221.school.service.application.usecase.HistoriqueTarifItem;
import com.ecole221.school.service.application.usecase.TarifActifResult;
import com.ecole221.school.service.infrastructure.web.dto.AffecterTarifRequest;
import com.ecole221.school.service.infrastructure.web.dto.CreerClasseRequest;
import com.ecole221.school.service.infrastructure.web.dto.HistoriqueTarifResponse;
import com.ecole221.school.service.infrastructure.web.dto.TarifActifResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Classe", description = "Gestion des classes")
@RequestMapping("/api/classes")
public class ClasseController {

    private final CreerClasseUseCase creerUC;
    private final AffecterTarifUseCase affecterTarifUC;
    private final ConsulterHistoriqueTarifsUseCase historiqueTarifsUC;
    private final ConsulterTarifActifUseCase tarifActifUC;

    public ClasseController(CreerClasseUseCase creerUC,
                             AffecterTarifUseCase affecterTarifUC,
                             ConsulterHistoriqueTarifsUseCase historiqueTarifsUC,
                             ConsulterTarifActifUseCase tarifActifUC) {
        this.creerUC = creerUC;
        this.affecterTarifUC = affecterTarifUC;
        this.historiqueTarifsUC = historiqueTarifsUC;
        this.tarifActifUC = tarifActifUC;
    }

    @PostMapping
    public ResponseEntity<Void> creer(@Valid @RequestBody CreerClasseRequest request) {
        UUID id = creerUC.executer(new CreerClasseCommand(
                request.code(),
                request.nom(),
                request.cycle(),
                request.niveau(),
                request.filiereId()
        ));
        return ResponseEntity.created(URI.create("/api/classes/" + id)).build();
    }

    @PostMapping("/{classeId}/tarif")
    public ResponseEntity<Void> affecterTarif(@PathVariable UUID classeId,
                                               @Valid @RequestBody AffecterTarifRequest request) {
        affecterTarifUC.executer(new AffecterTarifCommand(classeId, request.tarifId()));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{classeId}/tarif-actif")
    public ResponseEntity<TarifActifResponse> tarifActif(@PathVariable UUID classeId) {
        TarifActifResult result = tarifActifUC.executer(classeId);
        return ResponseEntity.ok(new TarifActifResponse(
                result.classeId(), result.code(), result.nom(), result.cycle(), result.niveau(),
                result.tarifId(), result.fraisInscription(), result.mensualite(), result.autresFrais()
        ));
    }

    @GetMapping("/{classeId}/tarifs/historique")
    public ResponseEntity<List<HistoriqueTarifResponse>> historiqueTarifs(@PathVariable UUID classeId) {
        List<HistoriqueTarifResponse> response = historiqueTarifsUC.executer(classeId)
                .stream()
                .map(ClasseController::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    private static HistoriqueTarifResponse toResponse(HistoriqueTarifItem item) {
        return new HistoriqueTarifResponse(
                item.tarifId(),
                item.fraisInscription(),
                item.mensualite(),
                item.autresFrais(),
                item.dateActivation(),
                item.dateDesactivation(),
                item.actif()
        );
    }
}
