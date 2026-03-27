package de.biketrip.advisor.api;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record PipelineRequest(
        @NotBlank String userMessage,
        Map<String, String> modelOverrides
) {}
