# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Biketrip Advisor — Multi-Agent LLM Showcase. A demo application that chains 4 specialized AI agents (Chat+RAG, Reasoning, Planning, Language) to create personalized bike trip plans. Each agent uses a different Ollama model to showcase LLM comparison.

## Build & Run Commands

### Prerequisites
- Java 21+
- Node.js 18+
- Ollama running locally (`ollama serve`)
- Required models: `ollama pull mistral deepseek-r1:8b qwen2.5:7b llama3.1:8b`

### Backend (Spring Boot + LangChain4j)
```bash
cd backend
mvn spring-boot:run          # Start on port 8080
mvn compile                  # Compile only
mvn test                     # Run tests
```

### Frontend (React + Vite + TypeScript)
```bash
cd frontend
npm install                  # Install dependencies
npm run dev                  # Dev server on port 5173
npm run build                # Production build
npx tsc --noEmit             # Type-check without emitting
```

## Architecture

### Pipeline Flow
```
User Request → [Chat+RAG] → [Reasoning] → [Planning] → [Geo-Routing] → [Language] → Markdown Report
                mistral     deepseek-r1:8b  qwen2.5:7b   Nominatim+ORS  llama3.1:8b
```

### Backend (`backend/src/main/java/de/biketrip/advisor/`)
- **`config/`** — Spring beans: `OllamaModelsConfig` (YAML binding), `LangChainConfig` (4 named ChatLanguageModel beans), `RagConfig` (embedding store + content retriever), `WebConfig` (CORS)
- **`agent/`** — The 4 LLM agents (`ChatAgent`, `ReasoningAgent`, `PlanningAgent`, `LanguageAgent`), `GeoRoutingService` (geocoding via Nominatim + routing via OpenRouteService), and `PipelineOrchestrator` which chains them sequentially. Each agent has a German system prompt and supports runtime model override.
- **`config/RoutingConfig.java`** — `@ConfigurationProperties` for OpenRouteService API key and base URL.
- **`rag/`** — `BikeRouteDataSeeder` loads text files from `resources/bike-routes/` into an in-memory embedding store at startup using `AllMiniLmL6V2EmbeddingModel` (in-process ONNX).
- **`api/`** — `PipelineController` with REST endpoints: `POST /api/pipeline/run` (sync), `POST /api/pipeline/run-streaming` (SSE), `GET /api/models`, `GET /api/config`.

### Frontend (`frontend/src/`)
- **`hooks/usePipeline.ts`** — Core state management. Parses SSE stream from backend via `fetch` + `ReadableStream`.
- **`components/pipeline/`** — `PipelineView`, `AgentStepCard` (shows each agent's input/output/model/duration), `StepProgress` (horizontal stepper).
- **`components/output/FinalReport.tsx`** — Renders the Language agent's Markdown output via `react-markdown`.
- **`components/output/RouteMap.tsx`** — Interactive Leaflet map with OSM tiles showing the bike route (GeoJSON from OpenRouteService) and day-markers with popups.
- **`components/input/`** — `TripRequestForm` (textarea), `ModelSelector` (4 dropdowns to override Ollama models per role).

### Key Design Decisions
- LangChain4j models are built programmatically (not `@AiService`) to support runtime model swapping via the frontend's model selector.
- Embeddings use in-process `AllMiniLmL6V2` (23MB ONNX) to avoid requiring a separate Ollama embedding model.
- SSE (not WebSocket) for pipeline progress — simpler, unidirectional, sufficient for 4 sequential events.
- `verbatimModuleSyntax: true` in tsconfig — all type-only imports must use `import type`.
- Virtual threads enabled for blocking Ollama calls.

### Docker
- **`backend/Dockerfile`** — Multi-stage: Maven build on `eclipse-temurin:21`, run on `eclipse-temurin:21-jre-alpine`.
- **`frontend/Dockerfile`** — Multi-stage: npm build on `node:22-alpine`, serve via `nginx:alpine`.
- **`frontend/nginx.conf`** — Reverse proxy: `/biketrip-advisor/*` → `backend:8080`, SPA fallback for everything else. `proxy_buffering off` for SSE streaming.
- **`docker-compose.yml`** — Starts `backend` + `frontend`. Ollama runs on host, reached via `host.docker.internal:11434`.

### Docker Run
```bash
docker compose up --build    # Build and start both services
```
App available at http://localhost:3000. Ollama must be running on the host.

### API Base URL
All backend endpoints live under the context path `/biketrip-advisor/` (e.g. `/biketrip-advisor/api/pipeline/run`). The frontend API base is configurable via `VITE_API_BASE`:
- **Local dev** (`npm run dev`): `frontend/.env` sets `VITE_API_BASE=http://localhost:8080/biketrip-advisor/api` (direct to backend)
- **Docker**: no env var needed, defaults to `/biketrip-advisor/api` (proxied through Nginx to backend container)

### Model Configuration
Default models are in `backend/src/main/resources/application.yml` under `biketrip.ollama.*`. All values are overridable via environment variables in the project root `.env` file (used by docker-compose):
- `BIKETRIP_OLLAMA_BASE_URL` — Ollama server URL
- `BIKETRIP_OLLAMA_CHAT_MODEL` — Chat+RAG agent model (default: `mistral`)
- `BIKETRIP_OLLAMA_REASONING_MODEL` — Reasoning agent model (default: `deepseek-r1:8b`)
- `BIKETRIP_OLLAMA_PLANNING_MODEL` — Planning agent model (default: `qwen2.5:7b`)
- `BIKETRIP_OLLAMA_LANGUAGE_MODEL` — Language agent model (default: `llama3.1:8b`)

The frontend can additionally override models per request via the ModelSelector dropdowns.

### Geo-Routing Configuration
- `OPENROUTESERVICE_API_KEY` — Free API key from https://openrouteservice.org/dev/#/signup (40 req/min)
- Without a key, the app still works but shows only waypoint markers (no calculated route line)
- Geocoding uses OSM Nominatim (free, no key, 1 req/s rate limit)
- The `GeoRoutingService` runs between Planning and Language stages, sends an SSE `route-ready` event so the map appears before the final report
