package com.ecole221.school.service.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "tarif")
public class TarifJpaEntity {

    @Id
    @Column(nullable = false, unique = true)
    private UUID id;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal fraisInscription;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal mensualite;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal autresFrais;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public BigDecimal getFraisInscription() { return fraisInscription; }
    public void setFraisInscription(BigDecimal fraisInscription) { this.fraisInscription = fraisInscription; }
    public BigDecimal getMensualite() { return mensualite; }
    public void setMensualite(BigDecimal mensualite) { this.mensualite = mensualite; }
    public BigDecimal getAutresFrais() { return autresFrais; }
    public void setAutresFrais(BigDecimal autresFrais) { this.autresFrais = autresFrais; }
}
