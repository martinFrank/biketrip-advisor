package de.biketrip.advisor.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class PipelineOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(PipelineOrchestrator.class);

    private final ChatAgent chatAgent;
    private final ReasoningAgent reasoningAgent;
    private final PlanningAgent planningAgent;
    private final LanguageAgent languageAgent;
    private final GeoRoutingService geoRoutingService;

    public PipelineOrchestrator(ChatAgent chatAgent,
                                 ReasoningAgent reasoningAgent,
                                 PlanningAgent planningAgent,
                                 LanguageAgent languageAgent,
                                 GeoRoutingService geoRoutingService) {
        this.chatAgent = chatAgent;
        this.reasoningAgent = reasoningAgent;
        this.planningAgent = planningAgent;
        this.languageAgent = languageAgent;
        this.geoRoutingService = geoRoutingService;
    }

    public PipelineResult execute(String userMessage, Map<String, String> modelOverrides) {
        return execute(userMessage, modelOverrides, step -> {}, route -> {});
    }

    public PipelineResult execute(String userMessage, Map<String, String> modelOverrides,
                                   Consumer<AgentStepResult> onStepComplete,
                                   Consumer<RouteResult> onRouteReady) {
        log.info("Pipeline: starting for message: {}", userMessage);
        List<AgentStepResult> steps = new ArrayList<>();

        // Stage 1: Chat with RAG
        AgentStepResult chatResult = chatAgent.process(userMessage, getOverride(modelOverrides, "CHAT"));
        steps.add(chatResult);
        onStepComplete.accept(chatResult);

        // Stage 2: Reasoning
        AgentStepResult reasonResult = reasoningAgent.process(chatResult.output(), getOverride(modelOverrides, "REASONING"));
        steps.add(reasonResult);
        onStepComplete.accept(reasonResult);

        // Stage 3: Planning
        AgentStepResult planResult = planningAgent.process(reasonResult.output(), getOverride(modelOverrides, "PLANNING"));
        steps.add(planResult);
        onStepComplete.accept(planResult);

        // Stage 3.5: Geo-Routing (between Planning and Language)
        RouteResult route = null;
        try {
            route = geoRoutingService.process(planResult.output());
            if (route != null) {
                onRouteReady.accept(route);
            }
        } catch (Exception e) {
            log.warn("Geo-routing failed, continuing without route: {}", e.getMessage());
        }

        // Stage 4: Language
        AgentStepResult langResult = languageAgent.process(planResult.output(), getOverride(modelOverrides, "LANGUAGE"));
        steps.add(langResult);
        onStepComplete.accept(langResult);

        log.info("Pipeline: completed all stages");
        return new PipelineResult(steps, langResult.output(), route);
    }

    private String getOverride(Map<String, String> overrides, String role) {
        if (overrides == null) return null;
        return overrides.get(role);
    }
}
