package com.ecole221.inscription.service.infrastructure.http;

import com.ecole221.inscription.service.application.port.out.EtudiantServicePort;
import com.ecole221.inscription.service.domain.exception.InscriptionException;
import com.ecole221.inscription.service.domain.exception.SchoolServiceIndisponibleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EtudiantServiceAdapter implements EtudiantServicePort {

    @Value("${etudiant-service.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    @Override
    public void supprimerEtudiant(UUID etudiantId) {
        String url = baseUrl + "/api/etudiants/id/" + etudiantId;
        try {
            restTemplate.delete(url);
            log.info("Compensation saga: étudiant {} supprimé", etudiantId);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("Compensation saga: étudiant {} introuvable (déjà supprimé ?)", etudiantId);
            } else {
                throw new SchoolServiceIndisponibleException(
                        "etudiant-service a retourné une erreur HTTP " + e.getStatusCode() + " pour l'étudiant " + etudiantId, e);
            }
        } catch (ResourceAccessException e) {
            throw new SchoolServiceIndisponibleException(
                    "etudiant-service est indisponible lors de la compensation pour l'étudiant " + etudiantId, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public UUID creerEtudiant(String nom, String prenom, LocalDate dateNaissance, String email, String codeAnnee) {
        String url = baseUrl + "/api/etudiants";
        Map<String, Object> body = Map.of(
                "nom", nom,
                "prenom", prenom,
                "dateNaissance", dateNaissance.toString(),
                "email", email,
                "codeAnnee", codeAnnee
        );
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, body, Map.class);
            if (response.getBody() == null || response.getBody().get("id") == null) {
                throw new InscriptionException("etudiant-service n'a pas retourné d'identifiant étudiant");
            }
            return UUID.fromString((String) response.getBody().get("id"));
        } catch (InscriptionException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST || e.getStatusCode() == HttpStatus.CONFLICT) {
                throw new InscriptionException("Impossible de créer l'étudiant : " + e.getResponseBodyAsString(), e);
            }
            throw new SchoolServiceIndisponibleException(
                    "etudiant-service a retourné une erreur HTTP " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {
            throw new SchoolServiceIndisponibleException(
                    "etudiant-service est indisponible lors de la création de l'étudiant", e);
        }
    }
}
