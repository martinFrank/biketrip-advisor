package de.biketrip.advisor.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LangChainConfig {

    @Bean("chatLlm")
    public ChatLanguageModel chatLlm(OllamaModelsConfig cfg) {
        return buildModel(cfg.baseUrl(), cfg.chatModel(), cfg.chatTemperature());
    }

    @Bean("reasoningLlm")
    public ChatLanguageModel reasoningLlm(OllamaModelsConfig cfg) {
        return buildModel(cfg.baseUrl(), cfg.reasoningModel(), cfg.reasoningTemperature());
    }

    @Bean("planningLlm")
    public ChatLanguageModel planningLlm(OllamaModelsConfig cfg) {
        return buildModel(cfg.baseUrl(), cfg.planningModel(), cfg.planningTemperature());
    }

    @Bean("languageLlm")
    public ChatLanguageModel languageLlm(OllamaModelsConfig cfg) {
        return buildModel(cfg.baseUrl(), cfg.languageModel(), cfg.languageTemperature());
    }

    public static ChatLanguageModel buildModel(String baseUrl, String modelName, double temperature) {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(Duration.ofMinutes(5))
                .build();
    }
}
