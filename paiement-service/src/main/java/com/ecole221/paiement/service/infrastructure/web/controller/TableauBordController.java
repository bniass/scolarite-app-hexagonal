package com.ecole221.paiement.service.infrastructure.web.controller;

import com.ecole221.paiement.service.application.port.in.TableauBordUseCase;
import com.ecole221.paiement.service.application.query.DossierResume;
import com.ecole221.paiement.service.application.query.LigneImpayee;
import com.ecole221.paiement.service.application.query.ResumeTableauBord;
import com.ecole221.paiement.service.domain.valueobject.StatutDossier;
import com.ecole221.paiement.service.infrastructure.web.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tableau-bord")
@RequiredArgsConstructor
public class TableauBordController {

    private final TableauBordUseCase tableauBordUC;

    /** Synthèse globale : totaux, montants, lignes impayées. */
    @GetMapping("/resume")
    public ResponseEntity<ResumeTableauBord> resume(
            @RequestParam String codeAnnee) {
        return ResponseEntity.ok(tableauBordUC.resume(codeAnnee));
    }

    /** Liste paginée des dossiers — filtre optionnel par statut. */
    @GetMapping("/dossiers")
    public ResponseEntity<PageResponse<DossierResume>> dossiers(
            @RequestParam String codeAnnee,
            @RequestParam(required = false) StatutDossier statut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<DossierResume> content = tableauBordUC.dossiers(codeAnnee, statut, page, size);
        long total = tableauBordUC.totalDossiers(codeAnnee, statut);
        return ResponseEntity.ok(new PageResponse<>(content, total, page, size));
    }

    /** Liste paginée des lignes non payées pour une année. */
    @GetMapping("/impayes")
    public ResponseEntity<PageResponse<LigneImpayee>> impayes(
            @RequestParam String codeAnnee,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<LigneImpayee> content = tableauBordUC.impayes(codeAnnee, page, size);
        long total = tableauBordUC.totalImpayes(codeAnnee);
        return ResponseEntity.ok(new PageResponse<>(content, total, page, size));
    }
}
