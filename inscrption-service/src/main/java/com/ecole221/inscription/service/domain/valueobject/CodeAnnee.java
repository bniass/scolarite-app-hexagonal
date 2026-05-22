package com.ecole221.inscription.service.domain.valueobject;

public class CodeAnnee {

    private final String value;

    public CodeAnnee(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Code année invalide");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
