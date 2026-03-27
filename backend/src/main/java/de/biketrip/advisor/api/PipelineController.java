package de.biketrip.advisor.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.biketrip.advisor.agent.PipelineOrchestrator;
import de.biketrip.advisor.agent.PipelineResult;
import de.biketrip.advisor.config.OllamaModelsConfig;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api")
public class PipelineController {

    private static final Logger log = LoggerFactory.getLogger(PipelineController.class);

    private final PipelineOrchestrator orchestrator;
    private final OllamaModelsConfig config;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public PipelineController(PipelineOrchestrator orchestrator,
                               OllamaModelsConfig config,
                               ObjectMapper objectMapper) {
        this.orchestrator = orchestrator;
        this.config = config;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/pipeline/run")
    public PipelineResult run(@Valid @RequestBody PipelineRequest request) {
        return orchestrator.execute(request.userMessage(), request.modelOverrides());
    }

    @PostMapping("/pipeline/run-streaming")
    public SseEmitter runStreaming(@Valid @RequestBody PipelineRequest request) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5 min timeout

        executor.submit(() -> {
            try {
                orchestrator.execute(request.userMessage(), request.modelOverrides(), step -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("step-complete")
                                .data(objectMapper.writeValueAsString(step), MediaType.APPLICATION_JSON));
                    } catch (Exception e) {
                        log.error("Error sending SSE event", e);
                    }
                }, route -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("route-ready")
                                .data(objectMapper.writeValueAsString(route), MediaType.APPLICATION_JSON));
                    } catch (Exception e) {
                        log.error("Error sending route SSE event", e);
                    }
                });
                emitter.send(SseEmitter.event().name("pipeline-complete").data("done"));
                emitter.complete();
            } catch (Exception e) {
                log.error("Pipeline execution failed", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @GetMapping("/models")
    public ResponseEntity<String> getModels() {
        try {
            String response = RestClient.create(config.baseUrl())
                    .get()
                    .uri("/api/tags")
                    .retrieve()
                    .body(String.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to fetch Ollama models", e);
            return ResponseEntity.internalServerError()
                    .body("{\"error\": \"Ollama not reachable at " + config.baseUrl() + "\"}");
        }
    }

    @GetMapping("/config")
    public Map<String, String> getConfig() {
        return Map.of(
                "CHAT", config.chatModel(),
                "REASONING", config.reasoningModel(),
                "PLANNING", config.planningModel(),
                "LANGUAGE", config.languageModel()
        );
    }
}
