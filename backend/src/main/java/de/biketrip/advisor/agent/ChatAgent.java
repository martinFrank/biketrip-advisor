package de.biketrip.advisor.agent;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import de.biketrip.advisor.config.LangChainConfig;
import de.biketrip.advisor.config.OllamaModelsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ChatAgent {

    private static final Logger log = LoggerFactory.getLogger(ChatAgent.class);

    private static final String SYSTEM_PROMPT = """
            Du bist ein erfahrener Fahrradtouren-Berater. Deine Aufgabe ist es, die Anfrage des Nutzers \
            zu verstehen und mit Hilfe der bereitgestellten Routeninformationen eine klare, \
            strukturierte Zusammenfassung zu erstellen.

            Extrahiere folgende Informationen:
            - Start- und Zielort
            - Anzahl der Tage
            - Budgetlimit
            - Besondere Wünsche (Sightseeing, Schwierigkeitsgrad, etc.)
            - Relevante Routeninformationen aus dem Kontext

            Antworte auf Deutsch. Gib eine strukturierte Zusammenfassung aus, keine Prosa.
            """;

    private final ChatLanguageModel defaultModel;
    private final ContentRetriever contentRetriever;
    private final OllamaModelsConfig config;

    public ChatAgent(@Qualifier("chatLlm") ChatLanguageModel defaultModel,
                     ContentRetriever contentRetriever,
                     OllamaModelsConfig config) {
        this.defaultModel = defaultModel;
        this.contentRetriever = contentRetriever;
        this.config = config;
    }

    public AgentStepResult process(String userMessage, String modelOverride) {
        log.info("ChatAgent: processing with RAG");

        ChatLanguageModel model = resolveModel(modelOverride);
        String modelName = modelOverride != null ? modelOverride : config.chatModel();

        // RAG: retrieve relevant context
        List<Content> retrieved = contentRetriever.retrieve(new Query(userMessage));
        String context = retrieved.stream()
                .map(c -> c.textSegment().text())
                .collect(Collectors.joining("\n\n"));

        String augmentedMessage = userMessage;
        if (!context.isBlank()) {
            augmentedMessage = """
                    ## Relevante Routeninformationen:
                    %s

                    ## Nutzeranfrage:
                    %s
                    """.formatted(context, userMessage);
        }

        long start = System.currentTimeMillis();
        AiMessage response = model.generate(
                SystemMessage.from(SYSTEM_PROMPT),
                UserMessage.from(augmentedMessage)
        ).content();
        long duration = System.currentTimeMillis() - start;

        log.info("ChatAgent: completed in {}ms", duration);
        return new AgentStepResult(AgentRole.CHAT, modelName, userMessage, response.text(), duration);
    }

    private ChatLanguageModel resolveModel(String override) {
        if (override == null || override.isBlank()) {
            return defaultModel;
        }
        return LangChainConfig.buildModel(config.baseUrl(), override, 0.7);
    }
}
