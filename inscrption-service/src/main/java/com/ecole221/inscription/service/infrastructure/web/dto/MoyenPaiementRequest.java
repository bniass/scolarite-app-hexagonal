package com.ecole221.inscription.service.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = MoyenPaiementRequest.MobileMoney.class,  name = "MOBILE_MONEY"),
        @JsonSubTypes.Type(value = MoyenPaiementRequest.Banque.class,       name = "BANQUE"),
        @JsonSubTypes.Type(value = MoyenPaiementRequest.Comptant.class,     name = "COMPTANT")
})
public sealed interface MoyenPaiementRequest
        permits MoyenPaiementRequest.MobileMoney,
                MoyenPaiementRequest.Banque,
                MoyenPaiementRequest.Comptant {

    String type();

    record MobileMoney(String operateur, String referencePaiement) implements MoyenPaiementRequest {
        @Override public String type() { return "MOBILE_MONEY"; }
    }

    record Banque(String nomBanque, String numeroTransaction) implements MoyenPaiementRequest {
        @Override public String type() { return "BANQUE"; }
    }

    record Comptant() implements MoyenPaiementRequest {
        @Override public String type() { return "COMPTANT"; }
    }
}
