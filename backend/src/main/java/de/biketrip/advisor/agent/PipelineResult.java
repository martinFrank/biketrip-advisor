package de.biketrip.advisor.agent;

import java.util.List;

public record PipelineResult(
        List<AgentStepResult> steps,
        String finalReport,
        RouteResult route
) {}
