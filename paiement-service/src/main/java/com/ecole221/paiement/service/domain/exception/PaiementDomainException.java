package com.ecole221.paiement.service.domain.exception;

public class PaiementDomainException extends RuntimeException {
    public PaiementDomainException(String message) { super(message); }
    public PaiementDomainException(String message, Throwable cause) { super(message, cause); }
}
