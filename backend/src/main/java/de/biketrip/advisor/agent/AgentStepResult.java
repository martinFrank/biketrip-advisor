package de.biketrip.advisor.agent;

public record AgentStepResult(
        AgentRole role,
        String modelUsed,
        String input,
        String output,
        long durationMs
) {}
