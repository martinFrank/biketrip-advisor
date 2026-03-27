package de.biketrip.advisor.agent;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import de.biketrip.advisor.config.OllamaModelsConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanningAgentTest {

    @Mock
    private ChatLanguageModel model;

    @Captor
    private ArgumentCaptor<ChatMessage> messageCaptor;

    private PlanningAgent agent;

    @BeforeEach
    void setUp() {
        OllamaModelsConfig config = new OllamaModelsConfig(
                "http://localhost:11434", "mistral", 0.7,
                "deepseek-r1:8b", 0.2, "qwen2.5:7b", 0.4, "llama3.1:8b", 0.8);
        agent = new PlanningAgent(model, config);
    }

    @Test
    void processReturnsCorrectRoleAndModel() {
        when(model.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(new Response<>(new AiMessage("Tag 1: Freiburg → Breisach")));

        AgentStepResult result = agent.process("Analyse-Ergebnis", null);

        assertThat(result.role()).isEqualTo(AgentRole.PLANNING);
        assertThat(result.modelUsed()).isEqualTo("qwen2.5:7b");
        assertThat(result.output()).isEqualTo("Tag 1: Freiburg → Breisach");
    }

    @Test
    void systemPromptContainsDayPlanStructure() {
        when(model.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(new Response<>(new AiMessage("ok")));

        agent.process("Input", null);

        verify(model).generate(messageCaptor.capture(), messageCaptor.capture());
        String systemPrompt = ((SystemMessage) messageCaptor.getAllValues().get(0)).text();
        assertThat(systemPrompt).contains("Tagesplan");
        assertThat(systemPrompt).contains("Distanz");
        assertThat(systemPrompt).contains("Unterkunft");
        assertThat(systemPrompt).contains("Gesamtkosten");
    }

    @Test
    void processPassesInputAsUserMessage() {
        when(model.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(new Response<>(new AiMessage("ok")));

        agent.process("Analyse der Bodenseetour", null);

        verify(model).generate(messageCaptor.capture(), messageCaptor.capture());
        String userMsg = ((UserMessage) messageCaptor.getAllValues().get(1)).singleText();
        assertThat(userMsg).isEqualTo("Analyse der Bodenseetour");
    }
}
