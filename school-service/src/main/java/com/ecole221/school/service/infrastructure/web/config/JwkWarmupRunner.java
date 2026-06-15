package com.ecole221.school.service.infrastructure.web.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class JwkWarmupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(JwkWarmupRunner.class);

    private final JwtDecoder jwtDecoder;

    public JwkWarmupRunner(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jwtDecoder.decode("warmup");
        } catch (Exception ignored) {
            // Token invalide attendu — l'objectif est uniquement de déclencher le fetch des JWK keys
            log.debug("JWK keys préchargées au démarrage");
        }
    }
}
