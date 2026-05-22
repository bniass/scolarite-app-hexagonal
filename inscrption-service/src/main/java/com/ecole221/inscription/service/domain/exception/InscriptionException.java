package com.ecole221.inscription.service.domain.exception;

import com.ecole221.common.exception.DomainException;

public class InscriptionException extends DomainException {

    public InscriptionException(String message) {
        super(message);
    }

    public InscriptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
