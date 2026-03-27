package de.biketrip.advisor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "biketrip.nominatim")
public record NominatimConfig(
        String baseUrl
) {}
