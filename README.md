# Biketrip Advisor

Multi-Agent LLM Showcase — eine Demo-Anwendung, die 4 spezialisierte KI-Agenten verkettet, um personalisierte Radtouren-Pläne zu erstellen.

## Showcase-Konzept

Die App demonstriert verschiedene LLM-Disziplinen in einer Pipeline:

| Stufe | Rolle | Modell | Aufgabe |
|-------|-------|--------|---------|
| 1 | Chat + RAG | mistral | Anfrage verstehen, Kontext aus Radwege-DB abrufen |
| 2 | Reasoning | deepseek-r1:8b | Machbarkeit, Risiken, Budget analysieren |
| 3 | Planning | qwen2.5:7b | Detaillierten Tagesplan erstellen |
| 4 | Language | llama3.1:8b | Plan in ansprechenden Markdown-Reisebericht umwandeln |

## Schnellstart

### 1. Ollama installieren und Modelle laden

```bash
# Ollama starten (https://ollama.ai)
ollama serve

# Modelle herunterladen
ollama pull mistral
ollama pull deepseek-r1:8b
ollama pull qwen2.5:7b
ollama pull llama3.1:8b
```

### 2. Backend starten

```bash
cd backend
mvn spring-boot:run
```

### 3. Frontend starten

```bash
cd frontend
npm install
npm run dev
```

### 4. Browser öffnen

http://localhost:5173

## Tech Stack

- **Backend**: Spring Boot 3.4 + LangChain4j 0.36 + Java 21
- **Frontend**: React + Vite + TypeScript
- **LLM Provider**: Ollama (lokal, keine API-Keys nötig)
- **RAG**: In-Memory EmbeddingStore + AllMiniLmL6V2 (in-process)
