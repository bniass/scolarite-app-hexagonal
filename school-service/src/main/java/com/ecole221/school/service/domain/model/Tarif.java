package com.ecole221.school.service.domain.model;

import com.ecole221.common.entity.AggregateRoot;
import com.ecole221.school.service.domain.event.TarifCreeEvent;
import com.ecole221.school.service.domain.exception.SchoolException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Tarif extends AggregateRoot<TarifId> {

    private BigDecimal fraisInscription;
    private BigDecimal mensualite;
    private BigDecimal autresFrais;

    public static Tarif creer(BigDecimal fraisInscription, BigDecimal mensualite, BigDecimal autresFrais) {
        valider(fraisInscription, mensualite, autresFrais);
        Tarif tarif = new Tarif();
        tarif.setId(TarifId.generate());
        tarif.fraisInscription = fraisInscription;
        tarif.mensualite = mensualite;
        tarif.autresFrais = autresFrais;
        tarif.addEvent(new TarifCreeEvent(
                tarif.getId().getValue().toString(),
                LocalDateTime.now()
        ));
        return tarif;
    }

    public static Tarif reconstituer(TarifId id, BigDecimal fraisInscription,
                                     BigDecimal mensualite, BigDecimal autresFrais) {
        Tarif tarif = new Tarif();
        tarif.setId(id);
        tarif.fraisInscription = fraisInscription;
        tarif.mensualite = mensualite;
        tarif.autresFrais = autresFrais;
        return tarif;
    }

    private static void valider(BigDecimal fraisInscription, BigDecimal mensualite, BigDecimal autresFrais) {
        if (fraisInscription == null || fraisInscription.compareTo(BigDecimal.ZERO) < 0) {
            throw new SchoolException("Les frais d'inscription doivent être positifs");
        }
        if (mensualite == null || mensualite.compareTo(BigDecimal.ZERO) < 0) {
            throw new SchoolException("La mensualité doit être positive");
        }
        if (autresFrais == null || autresFrais.compareTo(BigDecimal.ZERO) < 0) {
            throw new SchoolException("Les autres frais doivent être positifs");
        }
    }

    public BigDecimal getFraisInscription() { return fraisInscription; }
    public BigDecimal getMensualite() { return mensualite; }
    public BigDecimal getAutresFrais() { return autresFrais; }
}
