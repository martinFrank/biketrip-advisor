package de.biketrip.advisor.agent;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
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
class LanguageAgentTest {

    @Mock
    private ChatLanguageModel model;

    @Captor
    private ArgumentCaptor<ChatMessage> messageCaptor;

    private LanguageAgent agent;

    @BeforeEach
    void setUp() {
        OllamaModelsConfig config = new OllamaModelsConfig(
                "http://localhost:11434", "mistral", 0.7,
                "deepseek-r1:8b", 0.2, "qwen2.5:7b", 0.4, "llama3.1:8b", 0.8);
        agent = new LanguageAgent(model, config);
    }

    @Test
    void processReturnsCorrectRoleAndModel() {
        when(model.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(new Response<>(new AiMessage("# Traumhafte Bodenseetour")));

        AgentStepResult result = agent.process("Tagesplan-Input", null);

        assertThat(result.role()).isEqualTo(AgentRole.LANGUAGE);
        assertThat(result.modelUsed()).isEqualTo("llama3.1:8b");
        assertThat(result.output()).isEqualTo("# Traumhafte Bodenseetour");
    }

    @Test
    void systemPromptContainsMarkdownInstructions() {
        when(model.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(new Response<>(new AiMessage("ok")));

        agent.process("Input", null);

        verify(model).generate(messageCaptor.capture(), messageCaptor.capture());
        String systemPrompt = ((SystemMessage) messageCaptor.getAllValues().get(0)).text();
        assertThat(systemPrompt).contains("Markdown");
        assertThat(systemPrompt).contains("Reise-Redakteur");
    }
}
