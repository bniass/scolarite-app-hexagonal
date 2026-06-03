package com.ecole221.paiement.service.infrastructure.web.controller;

import com.ecole221.paiement.service.application.command.DistribuerVersementCommand;
import com.ecole221.paiement.service.application.port.in.ConsulterDossierUseCase;
import com.ecole221.paiement.service.application.port.in.DistribuerVersementUseCase;
import com.ecole221.paiement.service.domain.exception.PaiementDomainException;
import com.ecole221.paiement.service.domain.model.DossierPaiement;
import com.ecole221.paiement.service.domain.model.LignePaiement;
import com.ecole221.paiement.service.domain.model.MoyenPaiement;
import com.ecole221.paiement.service.domain.valueobject.TypeLigne;
import com.ecole221.paiement.service.infrastructure.web.dto.DossierPaiementResponse;
import com.ecole221.paiement.service.infrastructure.web.dto.DistribuerVersementRequest;
import com.ecole221.paiement.service.infrastructure.web.dto.LignePaiementResponse;
import com.ecole221.paiement.service.infrastructure.web.dto.MoyenPaiementRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.UUID;

@RestController
@RequestMapping("/api/dossiers")
@RequiredArgsConstructor
public class DossierPaiementController {

    private final ConsulterDossierUseCase consulterUC;
    private final DistribuerVersementUseCase distribuerUC;

    @GetMapping("/{inscriptionId}")
    public ResponseEntity<DossierPaiementResponse> consulter(@PathVariable UUID inscriptionId) {
        return ResponseEntity.ok(DossierPaiementResponse.from(consulterUC.parInscriptionId(inscriptionId)));
    }

    @GetMapping("/{inscriptionId}/prochaine-ligne")
    public ResponseEntity<LignePaiementResponse> prochaineLigne(@PathVariable UUID inscriptionId) {
        DossierPaiement dossier = consulterUC.parInscriptionId(inscriptionId);
        LignePaiement prochaine = dossier.getLignes().stream()
                .filter(l -> !l.estPayee())
                .min(Comparator.comparingInt(l -> {
                    if (l.getType() == TypeLigne.FRAIS_INSCRIPTION) return -2;
                    if (l.getType() == TypeLigne.AUTRES_FRAIS) return -1;
                    return l.getOrdreReglement();
                }))
                .orElseThrow(() -> new PaiementDomainException("Toutes les lignes sont déjà payées"));
        return ResponseEntity.ok(LignePaiementResponse.from(prochaine));
    }

    @PostMapping("/{inscriptionId}/versements/distribuer")
    public ResponseEntity<DossierPaiementResponse> distribuerVersement(
            @PathVariable UUID inscriptionId,
            @Valid @RequestBody DistribuerVersementRequest request) {

        DistribuerVersementCommand command = new DistribuerVersementCommand(
                inscriptionId,
                request.montant(),
                request.datePaiement(),
                toMoyenDomain(request.moyen())
        );

        DossierPaiement dossier = distribuerUC.executer(command);
        return ResponseEntity.ok(DossierPaiementResponse.from(dossier));
    }

    private MoyenPaiement toMoyenDomain(MoyenPaiementRequest r) {
        return switch (r) {
            case MoyenPaiementRequest.MobileMoney mm ->
                    MoyenPaiement.mobileMoney(mm.operateur(), mm.referencePaiement());
            case MoyenPaiementRequest.Banque b ->
                    MoyenPaiement.banque(b.nomBanque(), b.numeroTransaction());
            case MoyenPaiementRequest.Comptant ignored ->
                    MoyenPaiement.comptant();
        };
    }
}
