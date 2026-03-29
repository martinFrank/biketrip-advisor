#!/bin/bash
# LLM Benchmark for Biketrip Advisor Agents
# Tests each model on Reasoning, Planning, and Language tasks
# Measures response time and captures output for quality comparison

OLLAMA_HOST="http://192.168.0.251:11434"
OUTPUT_DIR="benchmark/results"
mkdir -p "$OUTPUT_DIR"

# Models that fit in 16GB VRAM
MODELS=(
  "mistral:7b"
  "llama3.1:8b"
  "qwen2.5:7b"
  "deepseek-r1:7b"
  "deepseek-r1:8b"
  "deepseek-r1:14b"
  "qwen3:8b"
  "qwen3:14b"
  "qwen3.5:9b"
)

# --- Fixed test inputs for each stage ---

# Input for Reasoning Agent (simulated ChatAgent output)
REASONING_INPUT='## Zusammenfassung der Touranfrage

**Start:** Nürnberg
**Ziel:** Regensburg
**Dauer:** 3 Tage
**Budget:** 500€
**Besondere Wünsche:** Sehenswürdigkeiten entlang der Route, mittlerer Schwierigkeitsgrad

### Relevante Routeninformationen:
- Der Fünf-Flüsse-Radweg verbindet Nürnberg und Regensburg über ca. 160 km
- Die Strecke führt entlang von Pegnitz, Vils, Naab und Donau
- Überwiegend flaches bis leicht hügeliges Terrain
- Gut ausgebaute Radwege, familienfreundlich
- Sehenswürdigkeiten: Nürnberger Altstadt, Amberg (historische Altstadt), Kallmünz (Künstlerdorf), Regensburger Dom und Steinerne Brücke'

# Input for Planning Agent (simulated ReasoningAgent output)
PLANNING_INPUT='## Analyse der Radtour Nürnberg → Regensburg

### 1. MACHBARKEIT
Die Strecke von ca. 160 km in 3 Tagen ergibt durchschnittlich ~53 km/Tag. Das liegt unter dem Richtwert von 60-80 km/Tag für Tourenradfahrer und ist daher **gut machbar**, auch für weniger erfahrene Radfahrer.

Empfohlene Tagesetappen:
- Tag 1: Nürnberg → Amberg (~60 km)
- Tag 2: Amberg → Kallmünz (~50 km)
- Tag 3: Kallmünz → Regensburg (~50 km)

### 2. RISIKEN
- **Wetter:** Ende Frühling/Sommer ideal. Bei Regen sind die Flusswege teilweise matschig.
- **Höhenprofil:** Überwiegend flach entlang der Flusstäler, kurze Anstiege zwischen Pegnitz- und Vilstal (~200 Höhenmeter gesamt).
- **Verkehr:** Größtenteils separate Radwege, nur kurze Abschnitte auf Landstraßen.
- **Versorgung:** Gute Infrastruktur, regelmäßige Ortschaften mit Gastronomie.

### 3. BUDGET-CHECK
- Unterkunft: 3 Nächte × 60€ = 180€ (Pensionen/Gasthöfe)
- Verpflegung: 3 Tage × 25€ = 75€
- Eintritt/Sightseeing: ~30€
- Notfallreserve: ~50€
- **Gesamt: ca. 335€** → Budget von 500€ ist **mehr als ausreichend**

### 4. ANFORDERUNGEN
- Tourenrad oder Trekkingrad empfohlen
- Grundfitness ausreichend (mittlerer Schwierigkeitsgrad bestätigt)
- Pannenwerkzeug und Regenkleidung einpacken
- Unterkünfte vorab reservieren (besonders in Kallmünz begrenzt)'

# Input for Language Agent (simulated PlanningAgent output)
LANGUAGE_INPUT='## Detaillierter Tourenplan: Nürnberg → Regensburg (3 Tage)

### Tag 1: Nürnberg → Amberg
- **Distanz:** 60 km
- **Fahrzeit:** 4-5 Stunden
- **Höhenprofil:** Flach bis leicht hügelig, Anstieg zwischen Pegnitz- und Vilstal
- **Sehenswürdigkeiten:**
  - Nürnberger Kaiserburg (Startpunkt)
  - Pegnitztal-Radweg mit Mühlen
  - Amberger Altstadt mit Stadtbrille (überbaute Brücke)
- **Unterkunft:** Gasthof Zum Löwen, Amberg (~55€/Nacht)
- **Verpflegung:** Mittagspause in Hersbruck (Fränkische Küche), Abendessen in Amberg

### Tag 2: Amberg → Kallmünz
- **Distanz:** 50 km
- **Fahrzeit:** 3-4 Stunden
- **Höhenprofil:** Flach entlang Vils und Naab
- **Sehenswürdigkeiten:**
  - Vilstal-Radweg durch idyllische Dörfer
  - Burgruine Kallmünz
  - Zusammenfluss von Vils und Naab
  - Künstlerdorf Kallmünz (Kandinsky malte hier)
- **Unterkunft:** Pension am Marktplatz, Kallmünz (~50€/Nacht)
- **Verpflegung:** Biergarten an der Naab, Abendessen in Kallmünz

### Tag 3: Kallmünz → Regensburg
- **Distanz:** 50 km
- **Fahrzeit:** 3-4 Stunden
- **Höhenprofil:** Flach entlang der Naab, dann Donau
- **Sehenswürdigkeiten:**
  - Naabtal-Radweg
  - Regensburger Dom (Gotik)
  - Steinerne Brücke (UNESCO-Welterbe)
  - Historische Wurstkuchl (älteste Bratwurstküche)
- **Unterkunft:** Hotel am Dom, Regensburg (~75€/Nacht)
- **Verpflegung:** Wurstkuchl am Donauufer, Abendessen in der Altstadt

### Gesamtkosten-Aufstellung
- Unterkünfte: 55€ + 50€ + 75€ = 180€
- Verpflegung: 3 × 25€ = 75€
- Eintritt/Sightseeing: ~30€
- Notfallreserve: 50€
- **Gesamt: ca. 335€** (Budget 500€ → 165€ Reserve)

### Packliste
- Helm, Fahrradschloss, Pannenwerkzeug
- Regenkleidung, Sonnenschutz
- Erste-Hilfe-Set
- Ladegeräte, Powerbank
- Radkarte/GPS

### Notfall-Tipps
- ADFC-Pannenhilfe: 030-2091580
- Euronotruf: 112
- Fahrradwerkstätten in Amberg und Regensburg'

# System prompts for each task
REASONING_SYSTEM='Du bist ein analytischer Experte für Radtouren-Planung. Analysiere die folgende Touranfrage gründlich:

1. MACHBARKEIT: Ist die Strecke in der angegebenen Zeit realistisch? Berechne Tagesetappen (ca. 60-80 km/Tag für Tourenradfahrer).
2. RISIKEN: Wetter, Höhenprofil, Verkehr, Grenzübergänge
3. BUDGET-CHECK: Ist das Budget realistisch? (Unterkunft ~40-80€/Nacht, Verpflegung ~20-30€/Tag)
4. ANFORDERUNGEN: Liste die konkreten Anforderungen strukturiert auf.

Denke Schritt für Schritt. Sei kritisch und ehrlich bei der Bewertung.
Antworte auf Deutsch.'

PLANNING_SYSTEM='Du bist ein Radtouren-Planer. Erstelle basierend auf der Analyse einen detaillierten Tagesplan im folgenden Format:

Für jeden Tag:
- Tag X: [Startort] → [Zielort]
- Distanz: XX km
- Fahrzeit: X Stunden
- Höhenprofil: [flach/hügelig/bergig]
- Sehenswürdigkeiten: [Liste]
- Unterkunft: [Vorschlag mit geschätzten Kosten]
- Verpflegung: [Tipps]

Am Ende:
- Gesamtkosten-Aufstellung
- Packliste-Empfehlung
- Notfall-Kontakte/Tipps

Antworte auf Deutsch. Verwende klare Struktur mit Aufzählungszeichen.'

LANGUAGE_SYSTEM='Du bist ein professioneller Reise-Redakteur. Verwandle den folgenden Tourenplan in einen ansprechenden, gut formatierten Markdown-Reisebericht.

Anforderungen:
- Verwende Markdown mit Überschriften (##, ###), Tabellen, und Emoji wo passend
- Schreibe einen einladenden Einleitungstext
- Formatiere die Tagesetappen als ansprechende Abschnitte
- Erstelle eine Kostenübersicht als Markdown-Tabelle
- Füge einen motivierenden Schlusssatz hinzu
- Der Bericht soll sich wie ein professioneller Reiseführer lesen

Antworte auf Deutsch. Nur Markdown-Ausgabe, kein Kommentar.'

# Function to run a single test
run_test() {
  local model="$1"
  local task="$2"
  local system_prompt="$3"
  local user_input="$4"
  local safe_model=$(echo "$model" | tr ':/' '_')
  local outfile="$OUTPUT_DIR/${safe_model}_${task}.json"
  local txtfile="$OUTPUT_DIR/${safe_model}_${task}.txt"

  echo "=== Testing $model on $task ==="

  # Build JSON payload
  local payload=$(jq -n \
    --arg model "$model" \
    --arg system "$system_prompt" \
    --arg user "$user_input" \
    '{
      model: $model,
      messages: [
        {role: "system", content: $system},
        {role: "user", content: $user}
      ],
      stream: false,
      options: {
        temperature: 0.7
      }
    }')

  local start_time=$(date +%s%N)

  # Call Ollama API with 5 minute timeout
  local response=$(curl -s --max-time 300 \
    "$OLLAMA_HOST/api/chat" \
    -H "Content-Type: application/json" \
    -d "$payload")

  local end_time=$(date +%s%N)
  local elapsed_ms=$(( (end_time - start_time) / 1000000 ))
  local elapsed_s=$(echo "scale=1; $elapsed_ms / 1000" | bc)

  # Extract metrics from response
  local content=$(echo "$response" | jq -r '.message.content // "ERROR"')
  local total_duration=$(echo "$response" | jq -r '.total_duration // 0')
  local eval_count=$(echo "$response" | jq -r '.eval_count // 0')
  local eval_duration=$(echo "$response" | jq -r '.eval_duration // 0')
  local prompt_eval_count=$(echo "$response" | jq -r '.prompt_eval_count // 0')
  local prompt_eval_duration=$(echo "$response" | jq -r '.prompt_eval_duration // 0')

  # Calculate tokens/sec
  local tokens_per_sec="0"
  if [ "$eval_duration" != "0" ] && [ "$eval_duration" != "null" ]; then
    tokens_per_sec=$(echo "scale=1; $eval_count / ($eval_duration / 1000000000)" | bc 2>/dev/null || echo "0")
  fi

  local total_duration_s="0"
  if [ "$total_duration" != "0" ] && [ "$total_duration" != "null" ]; then
    total_duration_s=$(echo "scale=1; $total_duration / 1000000000" | bc 2>/dev/null || echo "0")
  fi

  local word_count=$(echo "$content" | wc -w | tr -d ' ')

  echo "$content" > "$txtfile"

  # Save metrics as JSON
  jq -n \
    --arg model "$model" \
    --arg task "$task" \
    --arg elapsed "$elapsed_s" \
    --arg total_duration "$total_duration_s" \
    --arg eval_count "$eval_count" \
    --arg tokens_per_sec "$tokens_per_sec" \
    --arg prompt_tokens "$prompt_eval_count" \
    --arg word_count "$word_count" \
    '{
      model: $model,
      task: $task,
      wall_time_s: ($elapsed | tonumber),
      total_duration_s: ($total_duration | tonumber),
      output_tokens: ($eval_count | tonumber),
      tokens_per_sec: ($tokens_per_sec | tonumber),
      prompt_tokens: ($prompt_tokens | tonumber),
      word_count: ($word_count | tonumber)
    }' > "$outfile"

  echo "  -> ${elapsed_s}s | ${eval_count} tokens | ${tokens_per_sec} tok/s | ${word_count} words"
}

# --- Main benchmark loop ---
echo "========================================"
echo "  Biketrip Advisor LLM Benchmark"
echo "  $(date)"
echo "  Host: $OLLAMA_HOST"
echo "========================================"
echo ""

TOTAL_TESTS=$((${#MODELS[@]} * 3))
CURRENT=0

for model in "${MODELS[@]}"; do
  echo ""
  echo "### Model: $model ###"

  CURRENT=$((CURRENT + 1))
  echo "[$CURRENT/$TOTAL_TESTS] Reasoning..."
  run_test "$model" "reasoning" "$REASONING_SYSTEM" "$REASONING_INPUT"

  CURRENT=$((CURRENT + 1))
  echo "[$CURRENT/$TOTAL_TESTS] Planning..."
  run_test "$model" "planning" "$PLANNING_SYSTEM" "$PLANNING_INPUT"

  CURRENT=$((CURRENT + 1))
  echo "[$CURRENT/$TOTAL_TESTS] Language..."
  run_test "$model" "language" "$LANGUAGE_SYSTEM" "$LANGUAGE_INPUT"
done

echo ""
echo "========================================"
echo "  Benchmark complete!"
echo "========================================"

# Generate summary CSV
echo ""
echo "Generating summary..."
SUMMARY="$OUTPUT_DIR/summary.csv"
echo "model,task,wall_time_s,total_duration_s,output_tokens,tokens_per_sec,prompt_tokens,word_count" > "$SUMMARY"

for f in "$OUTPUT_DIR"/*.json; do
  jq -r '[.model, .task, .wall_time_s, .total_duration_s, .output_tokens, .tokens_per_sec, .prompt_tokens, .word_count] | @csv' "$f" >> "$SUMMARY"
done

echo "Summary saved to $SUMMARY"
echo "Individual outputs in $OUTPUT_DIR/*.txt"
