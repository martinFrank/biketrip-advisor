package de.biketrip.advisor.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LangChainConfig {

    private static final Logger log = LoggerFactory.getLogger(LangChainConfig.class);

    @Bean("chatLlm")
    public ChatLanguageModel chatLlm(OllamaModelsConfig cfg) {
        log.info("Initializing Chat LLM: model={}, temperature={}, baseUrl={}", cfg.chatModel(), cfg.chatTemperature(), cfg.baseUrl());
        return buildModel(cfg.baseUrl(), cfg.chatModel(), cfg.chatTemperature());
    }

    @Bean("reasoningLlm")
    public ChatLanguageModel reasoningLlm(OllamaModelsConfig cfg) {
        log.info("Initializing Reasoning LLM: model={}, temperature={}", cfg.reasoningModel(), cfg.reasoningTemperature());
        return buildModel(cfg.baseUrl(), cfg.reasoningModel(), cfg.reasoningTemperature());
    }

    @Bean("planningLlm")
    public ChatLanguageModel planningLlm(OllamaModelsConfig cfg) {
        log.info("Initializing Planning LLM: model={}, temperature={}", cfg.planningModel(), cfg.planningTemperature());
        return buildModel(cfg.baseUrl(), cfg.planningModel(), cfg.planningTemperature());
    }

    @Bean("languageLlm")
    public ChatLanguageModel languageLlm(OllamaModelsConfig cfg) {
        log.info("Initializing Language LLM: model={}, temperature={}", cfg.languageModel(), cfg.languageTemperature());
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
