package de.biketrip.advisor.agent;

/**
 * Common interface for all pipeline agents.
 * Enables configurable pipeline composition and testability.
 */
public interface PipelineAgent {

    AgentStepResult process(String input, String modelOverride);

    AgentRole getRole();
}
