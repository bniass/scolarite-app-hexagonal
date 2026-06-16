package com.ecole221.paiement.service.domain.model;

import com.ecole221.common.entity.AggregateRoot;
import com.ecole221.paiement.service.domain.event.DossierInitialiseEvent;
import com.ecole221.paiement.service.domain.exception.PaiementDomainException;
import com.ecole221.paiement.service.domain.valueobject.StatutDossier;
import com.ecole221.paiement.service.domain.valueobject.TypeLigne;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class DossierPaiement extends AggregateRoot<UUID> {

    private UUID inscriptionId;
    private UUID etudiantId;
    private UUID classeId;
    private String codeAnnee;
    private BigDecimal fraisInscription;
    private BigDecimal mensualite;
    private BigDecimal autresFrais;
    private StatutDossier statut;
    private List<LignePaiement> lignes;

    private DossierPaiement() {}

    // Ordre de paiement: juin=1, oct=2, nov=3, dec=4, jan=5, fev=6, mar=7, avr=8, mai=9
    private static final Map<Integer, Integer> ORDRE_MOIS = Map.of(
            6, 1, 10, 2, 11, 3, 12, 4, 1, 5, 2, 6, 3, 7, 4, 8, 5, 9
    );

    public static DossierPaiement initialiser(UUID inscriptionId, UUID etudiantId, UUID classeId,
            String codeAnnee, BigDecimal fraisInscription, BigDecimal mensualite,
            BigDecimal autresFrais, List<MoisAcademique> moisAcademiques) {

        DossierPaiement d = new DossierPaiement();
        d.setId(UUID.randomUUID());
        d.inscriptionId = inscriptionId;
        d.etudiantId = etudiantId;
        d.classeId = classeId;
        d.codeAnnee = codeAnnee;
        d.fraisInscription = fraisInscription;
        d.mensualite = mensualite;
        d.autresFrais = autresFrais;
        d.statut = StatutDossier.INITIALISE;
        d.lignes = d.genererLignes(moisAcademiques);
        return d;
    }

    public static DossierPaiement reconstituer(UUID id, UUID inscriptionId, UUID etudiantId,
            UUID classeId, String codeAnnee, BigDecimal fraisInscription, BigDecimal mensualite,
            BigDecimal autresFrais, StatutDossier statut, List<LignePaiement> lignes) {
        DossierPaiement d = new DossierPaiement();
        d.setId(id);
        d.inscriptionId = inscriptionId;
        d.etudiantId = etudiantId;
        d.classeId = classeId;
        d.codeAnnee = codeAnnee;
        d.fraisInscription = fraisInscription;
        d.mensualite = mensualite;
        d.autresFrais = autresFrais;
        d.statut = statut;
        d.lignes = lignes != null ? new ArrayList<>(lignes) : new ArrayList<>();
        return d;
    }

    /**
     * Distribue automatiquement un montant global sur les lignes non payées,
     * dans l'ordre naturel : FRAIS_INSCRIPTION → AUTRES_FRAIS → MENSUALITE (par ordre).
     * Le surplus d'une ligne est reversé sur la suivante jusqu'à épuisement.
     *
     * Règles :
     * - Premier versement (statut INITIALISE) : montant >= fraisInscription + autresFrais + mensualite
     * - Toujours : montant <= total restant à payer sur l'année
     */
    public void distribuerVersement(BigDecimal montantTotal, LocalDate date, MoyenPaiement moyen) {
        if (statut == StatutDossier.CLOTURE) {
            throw new PaiementDomainException("Le dossier est clôturé");
        }
        if (montantTotal == null || montantTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaiementDomainException("Le montant à distribuer doit être positif");
        }

        // Premier versement : doit couvrir au minimum fraisInscription + autresFrais + 1 mensualité
        if (statut == StatutDossier.INITIALISE) {
            BigDecimal minimum = fraisInscription.add(autresFrais).add(mensualite);
            if (montantTotal.compareTo(minimum) < 0) {
                throw new PaiementDomainException(
                        "Le premier versement (" + montantTotal + ") doit couvrir au minimum les frais d'inscription"
                        + " + autres frais + 1 mensualité (" + minimum + ")");
            }
        }

        // Plafond : on ne peut pas payer plus que ce qui reste
        BigDecimal totalRestant = getTotalRestant();
        if (montantTotal.compareTo(totalRestant) > 0) {
            throw new PaiementDomainException(
                    "Le montant versé (" + montantTotal + ") dépasse le total restant à payer (" + totalRestant + ")");
        }

        List<LignePaiement> lignesOrdered = lignes.stream()
                .filter(l -> !l.estPayee())
                .sorted(Comparator.comparingInt(l -> {
                    if (l.getType() == TypeLigne.FRAIS_INSCRIPTION) return -2;
                    if (l.getType() == TypeLigne.AUTRES_FRAIS) return -1;
                    return l.getOrdreReglement();
                }))
                .toList();

        BigDecimal restant = montantTotal;
        for (LignePaiement ligne : lignesOrdered) {
            if (restant.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal aAppliquer = restant.min(ligne.getMontantRestant());
            Versement v = Versement.creer(aAppliquer, date, moyen);
            ligne.appliquerVersement(v);
            restant = restant.subtract(aAppliquer);
        }

        mettreAJourStatut();
    }

    /** Montant total encore dû sur toutes les lignes. */
    public BigDecimal getTotalRestant() {
        return lignes.stream()
                .map(LignePaiement::getMontantRestant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void mettreAJourStatut() {
        if (statut == StatutDossier.INITIALISE) {
            statut = StatutDossier.ACTIF;
            // Premier versement reçu → l'inscription peut être confirmée
            addEvent(new DossierInitialiseEvent(
                    inscriptionId.toString(), "CONFIRME", "", LocalDateTime.now()));
        }
        if (lignes.stream().allMatch(LignePaiement::estPayee)) {
            statut = StatutDossier.CLOTURE;
        }
    }

    private List<LignePaiement> genererLignes(List<MoisAcademique> moisAcademiques) {
        List<LignePaiement> result = new ArrayList<>();

        result.add(LignePaiement.creer(TypeLigne.FRAIS_INSCRIPTION, null, 0, fraisInscription,
                "Frais d'inscription " + codeAnnee));
        result.add(LignePaiement.creer(TypeLigne.AUTRES_FRAIS, null, 0, autresFrais,
                "Autres frais " + codeAnnee));

        for (MoisAcademique mois : moisAcademiques) {
            int ordre = ORDRE_MOIS.getOrDefault(mois.mois(), 99);
            String label = nomMois(mois.mois()) + " " + mois.annee();
            result.add(LignePaiement.creer(TypeLigne.MENSUALITE, mois, ordre, mensualite, label));
        }

        result.sort(Comparator.comparingInt(LignePaiement::getOrdreReglement));
        return result;
    }

    private static String nomMois(int mois) {
        return switch (mois) {
            case 1 -> "Janvier"; case 2 -> "Février"; case 3 -> "Mars";
            case 4 -> "Avril"; case 5 -> "Mai"; case 6 -> "Juin";
            case 7 -> "Juillet"; case 8 -> "Août"; case 9 -> "Septembre";
            case 10 -> "Octobre"; case 11 -> "Novembre"; case 12 -> "Décembre";
            default -> "Mois " + mois;
        };
    }

    public UUID getInscriptionId() { return inscriptionId; }
    public UUID getEtudiantId() { return etudiantId; }
    public UUID getClasseId() { return classeId; }
    public String getCodeAnnee() { return codeAnnee; }
    public BigDecimal getFraisInscription() { return fraisInscription; }
    public BigDecimal getMensualite() { return mensualite; }
    public BigDecimal getAutresFrais() { return autresFrais; }
    public StatutDossier getStatut() { return statut; }
    public List<LignePaiement> getLignes() { return Collections.unmodifiableList(lignes); }
}
