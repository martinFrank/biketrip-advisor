package de.biketrip.advisor.api;

import de.biketrip.advisor.agent.AgentRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record PipelineRequest(
        @NotBlank @Size(max = 5000, message = "Nachricht darf maximal 5000 Zeichen lang sein")
        String userMessage,
        Map<String, String> modelOverrides
) {
    private static final Set<String> VALID_ROLES = Stream.of(AgentRole.values())
            .map(Enum::name)
            .collect(Collectors.toSet());

    /**
     * Returns sanitized model overrides containing only valid agent role keys.
     * Unknown keys are silently dropped.
     */
    public Map<String, String> validatedOverrides() {
        if (modelOverrides == null || modelOverrides.isEmpty()) {
            return Map.of();
        }
        return modelOverrides.entrySet().stream()
                .filter(e -> VALID_ROLES.contains(e.getKey()))
                .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                .filter(e -> e.getValue().length() <= 100)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
