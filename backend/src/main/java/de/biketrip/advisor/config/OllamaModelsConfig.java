package de.biketrip.advisor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "biketrip.ollama")
public record OllamaModelsConfig(
        String baseUrl,
        String chatModel,
        double chatTemperature,
        String reasoningModel,
        double reasoningTemperature,
        String planningModel,
        double planningTemperature,
        String languageModel,
        double languageTemperature
) {}
