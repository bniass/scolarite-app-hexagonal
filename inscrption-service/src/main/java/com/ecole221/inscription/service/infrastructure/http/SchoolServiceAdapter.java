package com.ecole221.inscription.service.infrastructure.http;

import com.ecole221.inscription.service.application.port.out.SchoolServicePort;
import com.ecole221.inscription.service.application.port.out.TarifActifResult;
import com.ecole221.inscription.service.domain.exception.InscriptionException;
import com.ecole221.inscription.service.domain.exception.SchoolServiceIndisponibleException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SchoolServiceAdapter implements SchoolServicePort {

    @Value("${school-service.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    @Override
    @SuppressWarnings("unchecked")
    public TarifActifResult getTarifActif(UUID classeId) {
        String url = baseUrl + "/api/classes/" + classeId + "/tarif-actif";
        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) {
                throw new InscriptionException("Tarif actif introuvable pour la classe " + classeId);
            }
            return new TarifActifResult(
                    UUID.fromString((String) response.get("tarifId")),
                    new BigDecimal(response.get("fraisInscription").toString()),
                    new BigDecimal(response.get("mensualite").toString()),
                    new BigDecimal(response.get("autresFrais").toString())
            );
        } catch (InscriptionException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new InscriptionException(
                        "Aucun tarif actif trouvé pour la classe " + classeId, e);
            }
            throw new SchoolServiceIndisponibleException(
                    "Erreur inattendue de school-service (HTTP " + e.getStatusCode() + ")", e);
        } catch (ResourceAccessException e) {
            // timeout ou connexion refusée
            throw new SchoolServiceIndisponibleException(
                    "school-service est indisponible, veuillez réessayer ultérieurement", e);
        } catch (Exception e) {
            throw new SchoolServiceIndisponibleException(
                    "Erreur lors de la communication avec school-service : " + e.getMessage(), e);
        }
    }
}
