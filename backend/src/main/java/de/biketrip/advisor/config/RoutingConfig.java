package de.biketrip.advisor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "biketrip.openrouteservice")
public record RoutingConfig(
        String apiKey,
        String baseUrl
) {}
