package de.biketrip.advisor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "biketrip.cors")
public record CorsConfig(
        String[] allowedOrigins
) {}
