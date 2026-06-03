package com.ecole221.inscription.service.application.usecase;

import com.ecole221.inscription.service.application.command.CreerInscriptionCommand;
import com.ecole221.inscription.service.application.port.in.CreerInscriptionUseCase;
import com.ecole221.inscription.service.application.port.out.*;
import com.ecole221.inscription.service.application.port.out.repository.AnneeAcademiqueProjectionRepository;
import com.ecole221.inscription.service.domain.event.InscriptionCreeeEvent;
import com.ecole221.inscription.service.domain.exception.InscriptionException;
import com.ecole221.inscription.service.domain.model.Inscription;
import com.ecole221.inscription.service.domain.valueobject.EtatAnnee;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreerInscriptionService implements CreerInscriptionUseCase {

    private final InscriptionRepository inscriptionRepository;
    private final InscriptionOutboxPort outboxPort;
    private final AnneeAcademiqueProjectionRepository anneeProjectionRepo;
    private final SchoolServicePort schoolServicePort;
    private final EtudiantServicePort etudiantServicePort;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Inscription executer(CreerInscriptionCommand command) {
        var projection = anneeProjectionRepo.findByCodeAnnee(command.codeAnnee())
                .orElseThrow(() -> new InscriptionException(
                        "Année académique introuvable : " + command.codeAnnee()));

        if (projection.getEtatAnnee() != EtatAnnee.INSCRIPTIONS_OUVERTES) {
            throw new InscriptionException(
                    "Les inscriptions ne sont pas ouvertes pour l'année " + command.codeAnnee());
        }

        TarifActifResult tarif = schoolServicePort.getTarifActif(command.classeId());

        int nbMois = compterMois(projection.getMoisAcademiquesJson());
        validerMontant(command.montant(), tarif, nbMois);  // montant > 0 et <= total annuel

        boolean etudiantNouveau = (command.etudiantId() == null);
        UUID etudiantId = resolveEtudiantId(command);

        Inscription inscription = Inscription.creer(
                etudiantId,
                etudiantNouveau,
                command.classeId(),
                command.codeAnnee(),
                tarif.fraisInscription(),
                tarif.mensualite(),
                tarif.autresFrais(),
                projection.getMoisAcademiquesJson(),
                command.montant(),
                command.typePaiement(),
                command.operateur(),
                command.referencePaiement(),
                command.nomBanque(),
                command.numeroTransaction()
        );

        Inscription saved = inscriptionRepository.sauvegarder(inscription);

        inscription.pullDomainEvents().forEach(event -> {
            if (event instanceof InscriptionCreeeEvent e) {
                outboxPort.sauvegarder(e);
            }
        });

        return saved;
    }

    private void validerMontant(BigDecimal montant, TarifActifResult tarif, int nbMois) {
        if (montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InscriptionException("Le montant versé doit être supérieur à 0");
        }

        BigDecimal maximum = tarif.fraisInscription()
                .add(tarif.autresFrais())
                .add(tarif.mensualite().multiply(BigDecimal.valueOf(nbMois)));

        if (montant.compareTo(maximum) > 0) {
            throw new InscriptionException(
                    "Le montant versé (" + montant + ") dépasse le total annuel (" + maximum + "). " +
                    "L'année comporte " + nbMois + " mensualité(s) de " + tarif.mensualite() + ".");
        }
    }

    private int compterMois(String moisAcademiquesJson) {
        try {
            List<Map<String, Object>> mois = objectMapper.readValue(
                    moisAcademiquesJson, new TypeReference<>() {});
            return mois.size();
        } catch (Exception e) {
            return 0; // JSON invalide → pas de mensualités → plafond = frais seuls
        }
    }

    private UUID resolveEtudiantId(CreerInscriptionCommand command) {
        if (command.etudiantId() != null) {
            return command.etudiantId();
        }
        return etudiantServicePort.creerEtudiant(
                command.nomEtudiant(),
                command.prenomEtudiant(),
                command.dateNaissanceEtudiant(),
                command.emailEtudiant(),
                command.codeAnnee()
        );
    }
}
