package com.ecole221.inscription.service;

import com.ecole221.inscription.service.infrastructure.web.dto.MoyenPaiementRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.ecole221")
@ConfigurationPropertiesScan("com.ecole221")
@EnableScheduling
public class InscriptionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(InscriptionServiceApplication.class, args);
    }
}
