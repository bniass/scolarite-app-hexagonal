package com.ecole221.inscription.service.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record NouvelEtudiantRequest(
        @NotBlank String nom,
        @NotBlank String prenom,
        @NotNull @Past LocalDate dateNaissance,
        @NotBlank @Email String email
) {}
