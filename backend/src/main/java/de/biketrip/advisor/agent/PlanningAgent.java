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
public class PlanningAgent {

    private static final Logger log = LoggerFactory.getLogger(PlanningAgent.class);

    private static final String SYSTEM_PROMPT = """
            Du bist ein Radtouren-Planer. Erstelle basierend auf der Analyse einen detaillierten \
            Tagesplan im folgenden Format:

            Für jeden Tag:
            - Tag X: [Startort] → [Zielort]
            - Distanz: XX km
            - Fahrzeit: X Stunden
            - Höhenprofil: [flach/hügelig/bergig]
            - Sehenswürdigkeiten: [Liste]
            - Unterkunft: [Vorschlag mit geschätzten Kosten]
            - Verpflegung: [Tipps]

            Am Ende:
            - Gesamtkosten-Aufstellung
            - Packliste-Empfehlung
            - Notfall-Kontakte/Tipps

            Antworte auf Deutsch. Verwende klare Struktur mit Aufzählungszeichen.
            """;

    private final ChatLanguageModel defaultModel;
    private final OllamaModelsConfig config;

    public PlanningAgent(@Qualifier("planningLlm") ChatLanguageModel defaultModel,
                         OllamaModelsConfig config) {
        this.defaultModel = defaultModel;
        this.config = config;
    }

    public AgentStepResult process(String input, String modelOverride) {
        log.info("PlanningAgent: creating day-by-day plan");

        ChatLanguageModel model = resolveModel(modelOverride);
        String modelName = modelOverride != null ? modelOverride : config.planningModel();

        long start = System.currentTimeMillis();
        AiMessage response = model.generate(
                SystemMessage.from(SYSTEM_PROMPT),
                UserMessage.from(input)
        ).content();
        long duration = System.currentTimeMillis() - start;

        log.info("PlanningAgent: completed in {}ms", duration);
        return new AgentStepResult(AgentRole.PLANNING, modelName, input, response.text(), duration);
    }

    private ChatLanguageModel resolveModel(String override) {
        if (override == null || override.isBlank()) {
            return defaultModel;
        }
        return LangChainConfig.buildModel(config.baseUrl(), override, 0.4);
    }
}
