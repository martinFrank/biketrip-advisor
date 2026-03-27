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

    private final List<PipelineAgent> agents;
    private final GeoRoutingService geoRoutingService;

    public PipelineOrchestrator(List<PipelineAgent> agents, GeoRoutingService geoRoutingService) {
        this.agents = agents;
        this.geoRoutingService = geoRoutingService;
        log.info("Pipeline configured with agents: {}",
                agents.stream().map(a -> a.getRole().name()).toList());
    }

    public PipelineResult execute(String userMessage, Map<String, String> modelOverrides) {
        return execute(userMessage, modelOverrides, step -> {}, route -> {});
    }

    public PipelineResult execute(String userMessage, Map<String, String> modelOverrides,
                                   Consumer<AgentStepResult> onStepComplete,
                                   Consumer<RouteResult> onRouteReady) {
        log.info("Pipeline: starting for message: {}", userMessage);
        List<AgentStepResult> steps = new ArrayList<>();
        RouteResult route = null;
        String currentInput = userMessage;

        for (PipelineAgent agent : agents) {
            // Run geo-routing after planning, before subsequent agents
            if (agent.getRole() == AgentRole.LANGUAGE && !steps.isEmpty()) {
                route = runGeoRouting(currentInput, onRouteReady);
            }

            String override = getOverride(modelOverrides, agent.getRole().name());
            AgentStepResult result = agent.process(currentInput, override);
            steps.add(result);
            onStepComplete.accept(result);
            currentInput = result.output();
        }

        String finalReport = steps.isEmpty() ? "" : steps.getLast().output();
        log.info("Pipeline: completed all stages");
        return new PipelineResult(steps, finalReport, route);
    }

    private RouteResult runGeoRouting(String planningOutput, Consumer<RouteResult> onRouteReady) {
        try {
            RouteResult route = geoRoutingService.process(planningOutput);
            if (route != null) {
                onRouteReady.accept(route);
            }
            return route;
        } catch (Exception e) {
            log.warn("Geo-routing failed, continuing without route: {}", e.getMessage());
            return null;
        }
    }

    private String getOverride(Map<String, String> overrides, String role) {
        if (overrides == null) return null;
        return overrides.get(role);
    }
}
