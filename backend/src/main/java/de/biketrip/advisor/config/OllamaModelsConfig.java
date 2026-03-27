package de.biketrip.advisor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "biketrip.ollama")
public record OllamaModelsConfig(
        String baseUrl,
        String chatModel,
        String reasoningModel,
        String planningModel,
        String languageModel
) {}
