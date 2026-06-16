package com.ecole221.paiement.service.domain.model;

import com.ecole221.paiement.service.domain.valueobject.TypeMoyen;

import java.util.UUID;

public class MoyenPaiement {
    private UUID id;
    private TypeMoyen type;
    private String operateur;
    private String referencePaiement;
    private String nomBanque;
    private String numeroTransaction;

    private MoyenPaiement() {}

    public static MoyenPaiement mobileMoney(String operateur, String reference) {
        MoyenPaiement m = new MoyenPaiement();
        m.id = UUID.randomUUID();
        m.type = TypeMoyen.MOBILE_MONEY;
        m.operateur = operateur;
        m.referencePaiement = reference;
        return m;
    }

    public static MoyenPaiement banque(String nomBanque, String numeroTransaction) {
        MoyenPaiement m = new MoyenPaiement();
        m.id = UUID.randomUUID();
        m.type = TypeMoyen.BANQUE;
        m.nomBanque = nomBanque;
        m.numeroTransaction = numeroTransaction;
        return m;
    }

    public static MoyenPaiement comptant() {
        MoyenPaiement m = new MoyenPaiement();
        m.id = UUID.randomUUID();
        m.type = TypeMoyen.COMPTANT;
        return m;
    }

    public static MoyenPaiement transfert() {
        MoyenPaiement m = new MoyenPaiement();
        m.id = UUID.randomUUID();
        m.type = TypeMoyen.TRANSFERT;
        return m;
    }

    /** Crée une copie avec un nouvel UUID — nécessaire quand le même moyen est distribué sur plusieurs versements. */
    public MoyenPaiement copier() {
        MoyenPaiement m = new MoyenPaiement();
        m.id = UUID.randomUUID();
        m.type = this.type;
        m.operateur = this.operateur;
        m.referencePaiement = this.referencePaiement;
        m.nomBanque = this.nomBanque;
        m.numeroTransaction = this.numeroTransaction;
        return m;
    }

    public static MoyenPaiement reconstituer(UUID id, TypeMoyen type, String operateur,
            String referencePaiement, String nomBanque, String numeroTransaction) {
        MoyenPaiement m = new MoyenPaiement();
        m.id = id;
        m.type = type;
        m.operateur = operateur;
        m.referencePaiement = referencePaiement;
        m.nomBanque = nomBanque;
        m.numeroTransaction = numeroTransaction;
        return m;
    }

    public UUID getId() { return id; }
    public TypeMoyen getType() { return type; }
    public String getOperateur() { return operateur; }
    public String getReferencePaiement() { return referencePaiement; }
    public String getNomBanque() { return nomBanque; }
    public String getNumeroTransaction() { return numeroTransaction; }
}
