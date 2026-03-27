package de.biketrip.advisor.agent;

import dev.langchain4j.model.chat.ChatLanguageModel;
import de.biketrip.advisor.config.OllamaModelsConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class PlanningAgent extends BaseAgent {

    public PlanningAgent(@Qualifier("planningLlm") ChatLanguageModel defaultModel,
                         OllamaModelsConfig config) {
        super(defaultModel, config, "prompts/planning.txt", config.planningModel(), config.planningTemperature());
    }

    @Override
    public AgentRole getRole() {
        return AgentRole.PLANNING;
    }
}
