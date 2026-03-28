package de.biketrip.advisor.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.biketrip.advisor.agent.PipelineOrchestrator;
import de.biketrip.advisor.agent.PipelineResult;
import de.biketrip.advisor.config.ModelCategoriesConfig;
import de.biketrip.advisor.config.OllamaModelsConfig;
import jakarta.annotation.PreDestroy;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class PipelineController {

    private static final Logger log = LoggerFactory.getLogger(PipelineController.class);

    private final PipelineOrchestrator orchestrator;
    private final OllamaModelsConfig config;
    private final ModelCategoriesConfig categoriesConfig;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public PipelineController(PipelineOrchestrator orchestrator,
                               OllamaModelsConfig config,
                               ModelCategoriesConfig categoriesConfig,
                               ObjectMapper objectMapper) {
        this.orchestrator = orchestrator;
        this.config = config;
        this.categoriesConfig = categoriesConfig;
        this.objectMapper = objectMapper;
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @PostMapping("/pipeline/run")
    public PipelineResult run(@Valid @RequestBody PipelineRequest request) {
        log.info("POST /pipeline/run: message length={}, overrides={}",
                request.userMessage().length(), request.validatedOverrides());
        long start = System.currentTimeMillis();
        PipelineResult result = orchestrator.execute(request.userMessage(), request.validatedOverrides());
        log.info("POST /pipeline/run: completed in {}ms, {} steps",
                System.currentTimeMillis() - start, result.steps().size());
        return result;
    }

    @PostMapping("/pipeline/run-streaming")
    public SseEmitter runStreaming(@Valid @RequestBody PipelineRequest request) {
        log.info("POST /pipeline/run-streaming: message length={}, overrides={}",
                request.userMessage().length(), request.validatedOverrides());
        SseEmitter emitter = new SseEmitter(300_000L); // 5 min timeout

        executor.submit(() -> {
            long start = System.currentTimeMillis();
            try {
                orchestrator.execute(request.userMessage(), request.validatedOverrides(), step -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("step-complete")
                                .data(objectMapper.writeValueAsString(step), MediaType.APPLICATION_JSON));
                    } catch (Exception e) {
                        log.error("Error sending SSE step event: {}", e.getMessage());
                    }
                }, route -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("route-ready")
                                .data(objectMapper.writeValueAsString(route), MediaType.APPLICATION_JSON));
                    } catch (Exception e) {
                        log.error("Error sending SSE route event: {}", e.getMessage());
                    }
                });
                log.info("POST /pipeline/run-streaming: completed in {}ms",
                        System.currentTimeMillis() - start);
                emitter.send(SseEmitter.event().name("pipeline-complete").data("done"));
                emitter.complete();
            } catch (Exception e) {
                log.error("Pipeline execution failed", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (Exception sendError) {
                    log.error("Failed to send error event to client: {}", sendError.getMessage());
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @GetMapping("/models")
    public ResponseEntity<String> getModels() {
        log.debug("GET /models: fetching from Ollama at {}", config.baseUrl());
        try {
            String response = RestClient.create(config.baseUrl())
                    .get()
                    .uri("/api/tags")
                    .retrieve()
                    .body(String.class);
            log.debug("GET /models: successfully fetched models from Ollama");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("GET /models: failed to fetch from Ollama at {}: {}", config.baseUrl(), e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("{\"error\": \"Ollama service is not reachable\"}");
        }
    }

    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        log.debug("GET /config: returning defaults and model categories");
        return Map.of(
                "defaults", Map.of(
                        "CHAT", config.chatModel(),
                        "REASONING", config.reasoningModel(),
                        "PLANNING", config.planningModel(),
                        "LANGUAGE", config.languageModel()
                ),
                "categories", categoriesConfig.toMap()
        );
    }
}
