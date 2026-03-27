package de.biketrip.advisor.agent;

import dev.langchain4j.model.chat.ChatLanguageModel;
import de.biketrip.advisor.config.OllamaModelsConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class ReasoningAgent extends BaseAgent {

    public ReasoningAgent(@Qualifier("reasoningLlm") ChatLanguageModel defaultModel,
                          OllamaModelsConfig config) {
        super(defaultModel, config, "prompts/reasoning.txt", config.reasoningModel(), config.reasoningTemperature());
    }

    @Override
    public AgentRole getRole() {
        return AgentRole.REASONING;
    }
}
