package de.biketrip.advisor.agent;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import de.biketrip.advisor.config.LangChainConfig;
import de.biketrip.advisor.config.OllamaModelsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ReasoningAgent {

    private static final Logger log = LoggerFactory.getLogger(ReasoningAgent.class);

    private static final String SYSTEM_PROMPT = """
            Du bist ein analytischer Experte für Radtouren-Planung. Analysiere die folgende \
            Touranfrage gründlich:

            1. MACHBARKEIT: Ist die Strecke in der angegebenen Zeit realistisch? Berechne \
               Tagesetappen (ca. 60-80 km/Tag für Tourenradfahrer).
            2. RISIKEN: Wetter, Höhenprofil, Verkehr, Grenzübergänge
            3. BUDGET-CHECK: Ist das Budget realistisch? (Unterkunft ~40-80€/Nacht, \
               Verpflegung ~20-30€/Tag)
            4. ANFORDERUNGEN: Liste die konkreten Anforderungen strukturiert auf.

            Denke Schritt für Schritt. Sei kritisch und ehrlich bei der Bewertung.
            Antworte auf Deutsch.
            """;

    private final ChatLanguageModel defaultModel;
    private final OllamaModelsConfig config;

    public ReasoningAgent(@Qualifier("reasoningLlm") ChatLanguageModel defaultModel,
                          OllamaModelsConfig config) {
        this.defaultModel = defaultModel;
        this.config = config;
    }

    public AgentStepResult process(String input, String modelOverride) {
        log.info("ReasoningAgent: analyzing feasibility");

        ChatLanguageModel model = resolveModel(modelOverride);
        String modelName = modelOverride != null ? modelOverride : config.reasoningModel();

        long start = System.currentTimeMillis();
        AiMessage response = model.generate(
                SystemMessage.from(SYSTEM_PROMPT),
                UserMessage.from(input)
        ).content();
        long duration = System.currentTimeMillis() - start;

        log.info("ReasoningAgent: completed in {}ms", duration);
        return new AgentStepResult(AgentRole.REASONING, modelName, input, response.text(), duration);
    }

    private ChatLanguageModel resolveModel(String override) {
        if (override == null || override.isBlank()) {
            return defaultModel;
        }
        return LangChainConfig.buildModel(config.baseUrl(), override, 0.2);
    }
}
