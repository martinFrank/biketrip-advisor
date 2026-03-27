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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReasoningAgentTest {

    @Mock
    private ChatLanguageModel model;

    @Captor
    private ArgumentCaptor<ChatMessage> messageCaptor;

    private ReasoningAgent agent;

    @BeforeEach
    void setUp() {
        OllamaModelsConfig config = new OllamaModelsConfig(
                "http://localhost:11434", "mistral", 0.7,
                "deepseek-r1:8b", 0.2, "qwen2.5:7b", 0.4, "llama3.1:8b", 0.8);
        agent = new ReasoningAgent(model, config);
    }

    @Test
    void processReturnsCorrectRoleAndModel() {
        when(model.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(new Response<>(new AiMessage("Analyse: machbar")));

        AgentStepResult result = agent.process("Tour-Zusammenfassung", null);

        assertThat(result.role()).isEqualTo(AgentRole.REASONING);
        assertThat(result.modelUsed()).isEqualTo("deepseek-r1:8b");
        assertThat(result.input()).isEqualTo("Tour-Zusammenfassung");
        assertThat(result.output()).isEqualTo("Analyse: machbar");
    }

    @Test
    void systemPromptContainsFeasibilityInstructions() {
        when(model.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(new Response<>(new AiMessage("ok")));

        agent.process("Input", null);

        verify(model).generate(messageCaptor.capture(), messageCaptor.capture());
        List<ChatMessage> messages = messageCaptor.getAllValues();

        String systemPrompt = ((SystemMessage) messages.get(0)).text();
        assertThat(systemPrompt).contains("MACHBARKEIT");
        assertThat(systemPrompt).contains("RISIKEN");
        assertThat(systemPrompt).contains("BUDGET-CHECK");
    }

    @Test
    void processPassesInputAsUserMessage() {
        when(model.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(new Response<>(new AiMessage("ok")));

        agent.process("Detaillierte Analyse bitte", null);

        verify(model).generate(messageCaptor.capture(), messageCaptor.capture());
        String userMsg = ((UserMessage) messageCaptor.getAllValues().get(1)).singleText();
        assertThat(userMsg).isEqualTo("Detaillierte Analyse bitte");
    }
}
