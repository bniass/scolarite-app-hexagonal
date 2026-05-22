package com.ecole221.anneeacademique.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.ecole221")//(exclude = {DataSourceAutoConfiguration.class})
@ConfigurationPropertiesScan("com.ecole221")
public class AnneeAcademiqueApplication {
    public static void main(String[] args) {
        SpringApplication.run(AnneeAcademiqueApplication.class, args);

    }
}