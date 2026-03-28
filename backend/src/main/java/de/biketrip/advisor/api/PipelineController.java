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
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api")
public class PipelineController {

    private static final Logger log = LoggerFactory.getLogger(PipelineController.class);

    private final PipelineOrchestrator orchestrator;
    private final OllamaModelsConfig config;
    private final ModelCategoriesConfig categoriesConfig;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final AtomicBoolean pipelineRunning = new AtomicBoolean(false);

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
    public ResponseEntity<?> run(@Valid @RequestBody PipelineRequest request) {
        if (!pipelineRunning.compareAndSet(false, true)) {
            log.warn("POST /pipeline/run: rejected, pipeline already running");
            return ResponseEntity.status(429)
                    .body(Map.of("error", "Ich bin noch am Denken... Bitte warte, bis die aktuelle Anfrage abgeschlossen ist."));
        }
        try {
            log.info("POST /pipeline/run: message length={}, overrides={}",
                    request.userMessage().length(), request.validatedOverrides());
            long start = System.currentTimeMillis();
            PipelineResult result = orchestrator.execute(request.userMessage(), request.validatedOverrides());
            log.info("POST /pipeline/run: completed in {}ms, {} steps",
                    System.currentTimeMillis() - start, result.steps().size());
            return ResponseEntity.ok(result);
        } finally {
            pipelineRunning.set(false);
        }
    }

    @PostMapping("/pipeline/run-streaming")
    public SseEmitter runStreaming(@Valid @RequestBody PipelineRequest request) {
        if (!pipelineRunning.compareAndSet(false, true)) {
            log.warn("POST /pipeline/run-streaming: rejected, pipeline already running");
            SseEmitter errorEmitter = new SseEmitter(0L);
            executor.submit(() -> {
                try {
                    errorEmitter.send(SseEmitter.event().name("error")
                            .data("Ich bin noch am Denken... Bitte warte, bis die aktuelle Anfrage abgeschlossen ist."));
                    errorEmitter.complete();
                } catch (Exception e) {
                    errorEmitter.completeWithError(e);
                }
            });
            return errorEmitter;
        }

        log.info("POST /pipeline/run-streaming: message length={}, overrides={}",
                request.userMessage().length(), request.validatedOverrides());
        SseEmitter emitter = new SseEmitter(300_000L); // 5 min timeout

        executor.submit(() -> {
            long start = System.currentTimeMillis();
            try {
                orchestrator.execute(request.userMessage(), request.validatedOverrides(), step -> {
                    try {
                        String json = objectMapper.writeValueAsString(step);
                        log.debug("SSE: sending step-complete for role={}, json length={}", step.role(), json.length());
                        emitter.send(SseEmitter.event()
                                .name("step-complete")
                                .data(json, MediaType.APPLICATION_JSON));
                        // Send a comment as flush signal
                        emitter.send(SseEmitter.event().comment("flush"));
                        log.debug("SSE: sent step-complete + flush for role={}", step.role());
                    } catch (Exception e) {
                        log.error("SSE: ERROR sending step event for role={}: {} ({})",
                                step.role(), e.getMessage(), e.getClass().getName(), e);
                    }
                }, route -> {
                    try {
                        String json = objectMapper.writeValueAsString(route);
                        log.debug("SSE: sending route-ready, json length={}", json.length());
                        emitter.send(SseEmitter.event()
                                .name("route-ready")
                                .data(json, MediaType.APPLICATION_JSON));
                        emitter.send(SseEmitter.event().comment("flush"));
                        log.debug("SSE: sent route-ready + flush");
                    } catch (Exception e) {
                        log.error("SSE: ERROR sending route event: {} ({})",
                                e.getMessage(), e.getClass().getName(), e);
                    }
                });
                log.debug("SSE: sending pipeline-complete");
                emitter.send(SseEmitter.event().name("pipeline-complete").data("done"));
                log.debug("SSE: calling emitter.complete(), total time={}ms",
                        System.currentTimeMillis() - start);
                emitter.complete();
            } catch (Exception e) {
                log.error("Pipeline execution failed: {} ({})", e.getMessage(), e.getClass().getName(), e);
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (Exception sendError) {
                    log.error("Failed to send error event to client: {}", sendError.getMessage());
                }
                emitter.completeWithError(e);
            } finally {
                pipelineRunning.set(false);
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
