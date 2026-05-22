package com.ecole221.kafka.service.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "kafka-consumer")
public class KafkaConsumerConfigData {
    private String keyDeserializer;
    private String valueDeserializer;
    private String autoOffsetReset;
    private Boolean batchListener;
    private Boolean autoStartup;
    private Boolean autoCommit;
    private Integer concurrencyLevel;
    private Integer sessionTimeoutMs;
    private Long pollTimeoutMs;
    private Boolean specificAvroReader;
}
