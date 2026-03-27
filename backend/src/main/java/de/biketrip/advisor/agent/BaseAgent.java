package de.biketrip.advisor.agent;

import de.biketrip.advisor.config.LangChainConfig;
import de.biketrip.advisor.config.OllamaModelsConfig;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Base class for LLM agents. Eliminates duplication of model resolution,
 * timing, and prompt loading across all agent implementations.
 */
public abstract class BaseAgent implements PipelineAgent {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final ChatLanguageModel defaultModel;
    private final OllamaModelsConfig config;
    private final String systemPrompt;
    private final String defaultModelName;
    private final double temperature;

    protected BaseAgent(ChatLanguageModel defaultModel,
                        OllamaModelsConfig config,
                        String promptResourcePath,
                        String defaultModelName,
                        double temperature) {
        this.defaultModel = defaultModel;
        this.config = config;
        this.systemPrompt = loadPrompt(promptResourcePath);
        this.defaultModelName = defaultModelName;
        this.temperature = temperature;
    }

    @Override
    public AgentStepResult process(String input, String modelOverride) {
        log.info("{}: processing", getRole().getDisplayName());

        ChatLanguageModel model = resolveModel(modelOverride);
        String modelName = (modelOverride != null && !modelOverride.isBlank()) ? modelOverride : defaultModelName;

        String userMessage = prepareInput(input);

        long start = System.currentTimeMillis();
        AiMessage response = model.generate(
                SystemMessage.from(systemPrompt),
                UserMessage.from(userMessage)
        ).content();
        long duration = System.currentTimeMillis() - start;

        log.info("{}: completed in {}ms", getRole().getDisplayName(), duration);
        return new AgentStepResult(getRole(), modelName, input, response.text(), duration);
    }

    /**
     * Hook for subclasses to augment the input before sending to the model.
     * Default implementation returns the input unchanged.
     */
    protected String prepareInput(String input) {
        return input;
    }

    protected String getSystemPrompt() {
        return systemPrompt;
    }

    private ChatLanguageModel resolveModel(String override) {
        if (override == null || override.isBlank()) {
            return defaultModel;
        }
        return LangChainConfig.buildModel(config.baseUrl(), override, temperature);
    }

    private static String loadPrompt(String resourcePath) {
        try {
            return new ClassPathResource(resourcePath).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load prompt from " + resourcePath, e);
        }
    }
}
