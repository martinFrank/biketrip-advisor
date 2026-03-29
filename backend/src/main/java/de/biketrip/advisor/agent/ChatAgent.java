package de.biketrip.advisor.agent;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import de.biketrip.advisor.config.OllamaModelsConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Order(1)
public class ChatAgent extends BaseAgent {

    private final ContentRetriever contentRetriever;

    public ChatAgent(@Qualifier("chatLlm") ChatLanguageModel defaultModel,
                     ContentRetriever contentRetriever,
                     OllamaModelsConfig config) {
        super(defaultModel, config, "prompts/chat.txt", config.chatModel(), config.chatTemperature());
        this.contentRetriever = contentRetriever;
    }

    @Override
    public AgentRole getRole() {
        return AgentRole.CHAT;
    }

    @Override
    protected String prepareInput(String input) {
        log.debug("RAG: retrieving context for query ({} chars)", input.length());
        List<Content> retrieved = contentRetriever.retrieve(new Query(input));
        log.info("RAG: retrieved {} relevant segments", retrieved.size());

        String context = retrieved.stream()
                .map(c -> c.textSegment().text())
                .collect(Collectors.joining("\n\n"));

        if (context.isBlank()) {
            log.info("RAG: no relevant context found, using raw input");
            return input;
        }

        log.debug("RAG: context length={} chars", context.length());
        return """
                ## Relevante Routeninformationen:
                %s

                ## Nutzeranfrage:
                %s
                """.formatted(context, input);
    }
}
