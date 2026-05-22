package com.ecole221.etudiant.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.ecole221")//(exclude = {DataSourceAutoConfiguration.class})
@ConfigurationPropertiesScan("com.ecole221")
@EnableScheduling
public class EtudiantServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EtudiantServiceApplication.class, args);
    }
}
