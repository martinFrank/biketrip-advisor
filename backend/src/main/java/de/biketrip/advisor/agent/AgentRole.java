package de.biketrip.advisor.agent;

public enum AgentRole {
    CHAT("Chat + RAG"),
    REASONING("Reasoning"),
    PLANNING("Planning"),
    LANGUAGE("Language");

    private final String displayName;

    AgentRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
