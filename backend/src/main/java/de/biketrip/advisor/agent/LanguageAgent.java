package de.biketrip.advisor.agent;

import dev.langchain4j.model.chat.ChatLanguageModel;
import de.biketrip.advisor.config.OllamaModelsConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(4)
public class LanguageAgent extends BaseAgent {

    public LanguageAgent(@Qualifier("languageLlm") ChatLanguageModel defaultModel,
                         OllamaModelsConfig config) {
        super(defaultModel, config, "prompts/language.txt", config.languageModel(), config.languageTemperature());
    }

    @Override
    public AgentRole getRole() {
        return AgentRole.LANGUAGE;
    }
}
