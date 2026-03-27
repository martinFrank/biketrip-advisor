package de.biketrip.advisor.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import de.biketrip.advisor.config.OllamaModelsConfig;
import de.biketrip.advisor.config.RoutingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PipelineOrchestratorTest {

    @Mock private ChatLanguageModel chatLlm;
    @Mock private ChatLanguageModel reasoningLlm;
    @Mock private ChatLanguageModel planningLlm;
    @Mock private ChatLanguageModel languageLlm;
    @Mock private ContentRetriever contentRetriever;

    private PipelineOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        OllamaModelsConfig config = new OllamaModelsConfig(
                "http://localhost:11434", "mistral", "deepseek-r1:8b", "qwen2.5:7b", "llama3.1:8b");

        ChatAgent chatAgent = new ChatAgent(chatLlm, contentRetriever, config);
        ReasoningAgent reasoningAgent = new ReasoningAgent(reasoningLlm, config);
        PlanningAgent planningAgent = new PlanningAgent(planningLlm, config);
        LanguageAgent languageAgent = new LanguageAgent(languageLlm, config);

        RoutingConfig routingConfig = new RoutingConfig("", "https://api.openrouteservice.org");
        GeoRoutingService geoRoutingService = new GeoRoutingService(routingConfig, new ObjectMapper());

        orchestrator = new PipelineOrchestrator(
                chatAgent, reasoningAgent, planningAgent, languageAgent, geoRoutingService);
    }

    @Test
    void executeChainsPipelineStepsSequentially() {
        when(contentRetriever.retrieve(any())).thenReturn(List.of());
        when(chatLlm.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(new Response<>(new AiMessage("Chat-Output")));
        when(reasoningLlm.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(new Response<>(new AiMessage("Reasoning-Output")));
        when(planningLlm.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(new Response<>(new AiMessage("Planning-Output")));
        when(languageLlm.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(new Response<>(new AiMessage("Final-Report")));

        PipelineResult result = orchestrator.execute("Nutzeranfrage", null);

        assertThat(result.steps()).hasSize(4);
        assertThat(result.steps().get(0).role()).isEqualTo(AgentRole.CHAT);
        assertThat(result.steps().get(1).role()).isEqualTo(AgentRole.REASONING);
        assertThat(result.steps().get(2).role()).isEqualTo(AgentRole.PLANNING);
        assertThat(result.steps().get(3).role()).isEqualTo(AgentRole.LANGUAGE);
        assertThat(result.finalReport()).isEqualTo("Final-Report");
    }

    @Test
    void pipelineChainsOutputOfEachStepToNextInput() {
        when(contentRetriever.retrieve(any())).thenReturn(List.of());
        when(chatLlm.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(new Response<>(new AiMessage("chat-result")));
        when(reasoningLlm.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(new Response<>(new AiMessage("reasoning-result")));
        when(planningLlm.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(new Response<>(new AiMessage("planning-result")));
        when(languageLlm.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(new Response<>(new AiMessage("language-result")));

        PipelineResult result = orchestrator.execute("Start", null);

        // Chat gets user input, each subsequent step gets the previous step's output
        assertThat(result.steps().get(0).input()).isEqualTo("Start");
        assertThat(result.steps().get(1).input()).isEqualTo("chat-result");
        assertThat(result.steps().get(2).input()).isEqualTo("reasoning-result");
        assertThat(result.steps().get(3).input()).isEqualTo("planning-result");
    }

    @Test
    void executeCallsStepCompleteCallbackForEachStep() {
        when(contentRetriever.retrieve(any())).thenReturn(List.of());
        when(chatLlm.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(new Response<>(new AiMessage("c")));
        when(reasoningLlm.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(new Response<>(new AiMessage("r")));
        when(planningLlm.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(new Response<>(new AiMessage("p")));
        when(languageLlm.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(new Response<>(new AiMessage("l")));

        List<AgentStepResult> callbackSteps = new ArrayList<>();

        orchestrator.execute("Test", null, callbackSteps::add, route -> {});

        assertThat(callbackSteps).hasSize(4);
        assertThat(callbackSteps.get(0).role()).isEqualTo(AgentRole.CHAT);
        assertThat(callbackSteps.get(3).role()).isEqualTo(AgentRole.LANGUAGE);
    }

    @Test
    void pipelineContinuesWhenGeoRoutingFindsNoLocations() {
        when(contentRetriever.retrieve(any())).thenReturn(List.of());
        when(chatLlm.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(new Response<>(new AiMessage("c")));
        when(reasoningLlm.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(new Response<>(new AiMessage("r")));
        when(planningLlm.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(new Response<>(new AiMessage("no route pattern here")));
        when(languageLlm.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(new Response<>(new AiMessage("final")));

        PipelineResult result = orchestrator.execute("Test", null);

        assertThat(result.steps()).hasSize(4);
        assertThat(result.route()).isNull();
        assertThat(result.finalReport()).isEqualTo("final");
    }

    @Test
    void eachStepReportsCorrectModelName() {
        when(contentRetriever.retrieve(any())).thenReturn(List.of());
        when(chatLlm.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(new Response<>(new AiMessage("c")));
        when(reasoningLlm.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(new Response<>(new AiMessage("r")));
        when(planningLlm.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(new Response<>(new AiMessage("p")));
        when(languageLlm.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(new Response<>(new AiMessage("l")));

        PipelineResult result = orchestrator.execute("Test", null);

        assertThat(result.steps().get(0).modelUsed()).isEqualTo("mistral");
        assertThat(result.steps().get(1).modelUsed()).isEqualTo("deepseek-r1:8b");
        assertThat(result.steps().get(2).modelUsed()).isEqualTo("qwen2.5:7b");
        assertThat(result.steps().get(3).modelUsed()).isEqualTo("llama3.1:8b");
    }
}
