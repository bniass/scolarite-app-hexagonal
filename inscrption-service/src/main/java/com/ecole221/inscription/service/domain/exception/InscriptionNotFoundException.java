package com.ecole221.inscription.service.domain.exception;

import com.ecole221.common.exception.DomainException;

public class InscriptionNotFoundException extends DomainException {

    public InscriptionNotFoundException(String message) {
        super(message);
    }
}
