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
public class LanguageAgent {

    private static final Logger log = LoggerFactory.getLogger(LanguageAgent.class);

    private static final String SYSTEM_PROMPT = """
            Du bist ein professioneller Reise-Redakteur. Verwandle den folgenden Tourenplan \
            in einen ansprechenden, gut formatierten Markdown-Reisebericht.

            Anforderungen:
            - Verwende Markdown mit Überschriften (##, ###), Tabellen, und Emoji wo passend
            - Schreibe einen einladenden Einleitungstext
            - Formatiere die Tagesetappen als ansprechende Abschnitte
            - Erstelle eine Kostenübersicht als Markdown-Tabelle
            - Füge einen motivierenden Schlusssatz hinzu
            - Der Bericht soll sich wie ein professioneller Reiseführer lesen

            Antworte auf Deutsch. Nur Markdown-Ausgabe, kein Kommentar.
            """;

    private final ChatLanguageModel defaultModel;
    private final OllamaModelsConfig config;

    public LanguageAgent(@Qualifier("languageLlm") ChatLanguageModel defaultModel,
                         OllamaModelsConfig config) {
        this.defaultModel = defaultModel;
        this.config = config;
    }

    public AgentStepResult process(String input, String modelOverride) {
        log.info("LanguageAgent: creating polished report");

        ChatLanguageModel model = resolveModel(modelOverride);
        String modelName = modelOverride != null ? modelOverride : config.languageModel();

        long start = System.currentTimeMillis();
        AiMessage response = model.generate(
                SystemMessage.from(SYSTEM_PROMPT),
                UserMessage.from(input)
        ).content();
        long duration = System.currentTimeMillis() - start;

        log.info("LanguageAgent: completed in {}ms", duration);
        return new AgentStepResult(AgentRole.LANGUAGE, modelName, input, response.text(), duration);
    }

    private ChatLanguageModel resolveModel(String override) {
        if (override == null || override.isBlank()) {
            return defaultModel;
        }
        return LangChainConfig.buildModel(config.baseUrl(), override, 0.8);
    }
}
