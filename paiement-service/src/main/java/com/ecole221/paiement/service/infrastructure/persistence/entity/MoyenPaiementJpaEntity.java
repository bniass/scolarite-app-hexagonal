package com.ecole221.paiement.service.infrastructure.persistence.entity;

import com.ecole221.paiement.service.domain.valueobject.TypeMoyen;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "moyen_paiement")
@Getter
@Setter
@NoArgsConstructor
public class MoyenPaiementJpaEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeMoyen type;

    private String operateur;

    @Column(name = "reference_paiement")
    private String referencePaiement;

    @Column(name = "nom_banque")
    private String nomBanque;

    @Column(name = "numero_transaction")
    private String numeroTransaction;
}
