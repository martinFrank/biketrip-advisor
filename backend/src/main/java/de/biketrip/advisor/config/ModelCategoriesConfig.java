package de.biketrip.advisor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "biketrip.model-categories")
public record ModelCategoriesConfig(
        List<String> chat,
        List<String> reasoning,
        List<String> planning,
        List<String> language
) {
    public Map<String, List<String>> toMap() {
        return Map.of(
                "CHAT", chat,
                "REASONING", reasoning,
                "PLANNING", planning,
                "LANGUAGE", language
        );
    }
}
