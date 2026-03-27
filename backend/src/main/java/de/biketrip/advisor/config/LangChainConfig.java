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
        return buildModel(cfg.baseUrl(), cfg.chatModel(), 0.7);
    }

    @Bean("reasoningLlm")
    public ChatLanguageModel reasoningLlm(OllamaModelsConfig cfg) {
        return buildModel(cfg.baseUrl(), cfg.reasoningModel(), 0.2);
    }

    @Bean("planningLlm")
    public ChatLanguageModel planningLlm(OllamaModelsConfig cfg) {
        return buildModel(cfg.baseUrl(), cfg.planningModel(), 0.4);
    }

    @Bean("languageLlm")
    public ChatLanguageModel languageLlm(OllamaModelsConfig cfg) {
        return buildModel(cfg.baseUrl(), cfg.languageModel(), 0.8);
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
