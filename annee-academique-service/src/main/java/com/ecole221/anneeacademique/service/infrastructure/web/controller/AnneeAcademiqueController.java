package com.ecole221.anneeacademique.service.infrastructure.web.controller;

import com.ecole221.anneeacademique.service.application.command.*;
import com.ecole221.anneeacademique.service.application.port.in.*;
import com.ecole221.anneeacademique.service.infrastructure.web.dto.AnneeAcademiqueResponse;
import com.ecole221.anneeacademique.service.infrastructure.web.mapper.AnneeAcademiqueMapper;
import com.ecole221.anneeacademique.service.infrastructure.web.dto.CreerAnneeAcademiqueRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@Tag(name = "AnneeAcademique", description = "Gestion de l'année académique")
@RequestMapping("/api/academic-years")
public class AnneeAcademiqueController {
    private final FermerInscriptionUseCase fermerUC;
    private final CreerAnneeAcademiqueUseCase creerUC;
    private final PublierAnneeAcademiqueUseCase publierUC;
    private final OuvrirInscriptionUseCase ouvrirUC;
    private final CloturerAnneeScolaireUseCase cloturerUC;
    private final ModifierAnneeAcademiqueUseCase modifierUC;
    private final ListerAnneesAcademiquesUseCase listerUC;

    public AnneeAcademiqueController(
            FermerInscriptionUseCase fermerUC,
            CreerAnneeAcademiqueUseCase creerUC,
            PublierAnneeAcademiqueUseCase publierUC,
            OuvrirInscriptionUseCase ouvrirUC,
            CloturerAnneeScolaireUseCase cloturerUC,
            ModifierAnneeAcademiqueUseCase modifierUC,
            ListerAnneesAcademiquesUseCase listerUC
    ) {
        this.fermerUC = fermerUC;
        this.creerUC = creerUC;
        this.publierUC = publierUC;
        this.ouvrirUC = ouvrirUC;
        this.cloturerUC = cloturerUC;
        this.modifierUC = modifierUC;
        this.listerUC = listerUC;
    }

    @GetMapping
    public ResponseEntity<List<AnneeAcademiqueResponse>> lister() {
        List<AnneeAcademiqueResponse> annees = listerUC.executer()
                .stream()
                .map(AnneeAcademiqueResponse::from)
                .toList();
        return ResponseEntity.ok(annees);
    }

    @PostMapping
    public ResponseEntity<Void> creer(@RequestBody CreerAnneeAcademiqueRequest request) {
        creerUC.executer(AnneeAcademiqueMapper.toCommand(request));

        URI location = URI.create("/api/academic-years/" + request.code());

        return ResponseEntity
                .created(location)
                .build();
    }


    @GetMapping("/{code}/publish")
    public ResponseEntity<Void> publier(@PathVariable String code) {
        publierUC.executer(new PublierAnneeAcademiqueCommand(Integer.parseInt(code)));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{code}/open-enrollments")
    public ResponseEntity<Void> ouvrirInscriptions(@PathVariable String code) {
        ouvrirUC.executer(new OuvrirInscriptionCommand(Integer.parseInt(code)));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{code}/close-enrollments")
    public ResponseEntity<Void> fermerInscriptions(@PathVariable String code) {
        fermerUC.executer(new FermerInscriptionCommand(Integer.parseInt(code)));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{code}/close")
    public ResponseEntity<Void> cloturer(@PathVariable String code) {
        cloturerUC.executer(new CloturerAnneeAcademiqueCommand(Integer.parseInt(code)));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{code}/update")
    public ResponseEntity<Void> modifier(@RequestBody ModifierAnneeAcademiqueCommand request) {
        modifierUC.executer(AnneeAcademiqueMapper.toModiferCommand(request));
        return ResponseEntity.ok().build();
    }

}

