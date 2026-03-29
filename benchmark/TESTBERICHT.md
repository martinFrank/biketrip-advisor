# LLM-Benchmark: Biketrip Advisor Agent-Aufgaben

**Datum:** 29.03.2026
**Hardware:** AMD Ryzen 7 9700X, NVIDIA RTX 4060 Ti (16GB VRAM), 96 GB DDR5 RAM
**Ollama Host:** 192.168.0.251:11434
**Quantisierung:** Alle Modelle Q4_K_M (GGUF)

## Testaufbau

Jedes Modell wurde mit den originalen System-Prompts der drei Pipeline-Agenten getestet:

| Aufgabe | Agent | Beschreibung |
|---------|-------|-------------|
| **Reasoning** | ReasoningAgent | Machbarkeitsanalyse, Risiken, Budget-Check, Anforderungen |
| **Planning** | PlanningAgent | Detaillierter Tagesplan mit Etappen, Kosten, Packliste |
| **Language** | LanguageAgent | Umwandlung in formatierten Markdown-Reisebericht |

**Testszenario:** 3-Tage-Radtour Nuernberg -> Regensburg, Budget 500 EUR, mittlerer Schwierigkeitsgrad.
Jede Aufgabe erhielt einen fixen Input (simulierter Output der vorherigen Stufe), um faire Vergleichbarkeit zu gewaehrleisten. Temperatur: 0.7.

---

## Ergebnisse: Geschwindigkeit

### Uebersicht aller Messungen

| Modell | Groesse | Aufgabe | Zeit (s) | Output-Tokens | Tok/s | Woerter |
|--------|---------|---------|----------|---------------|-------|---------|
| mistral:7b | 7.2B | Reasoning | 11.5 | 599 | 58.7 | 235 |
| mistral:7b | 7.2B | Planning | 16.7 | 948 | 57.8 | 298 |
| mistral:7b | 7.2B | Language | 14.4 | 804 | 57.5 | 248 |
| llama3.1:8b | 8.0B | Reasoning | 14.8 | 733 | 54.7 | 334 |
| llama3.1:8b | 8.0B | Planning | 13.1 | 686 | 54.4 | 266 |
| llama3.1:8b | 8.0B | Language | 17.2 | 894 | 53.9 | 310 |
| qwen2.5:7b | 7.6B | Reasoning | 21.9 | 1147 | 58.0 | 433 |
| qwen2.5:7b | 7.6B | Planning | 16.2 | 905 | 57.9 | 312 |
| qwen2.5:7b | 7.6B | Language | 20.7 | 1148 | 57.5 | 442 |
| deepseek-r1:7b | 7.6B | Reasoning | 24.0 | 1225 | 58.0 | 235 |
| deepseek-r1:7b | 7.6B | Planning | 21.0 | 1177 | 57.7 | 233 |
| deepseek-r1:7b | 7.6B | Language | 31.3 | 1740 | 57.3 | 402 |
| deepseek-r1:8b | 8.2B | Reasoning | 52.7 | 2514 | 49.7 | 759 |
| deepseek-r1:8b | 8.2B | Planning | 38.5 | 1891 | 49.8 | 341 |
| deepseek-r1:8b | 8.2B | Language | 37.9 | 1843 | 49.4 | 651 |
| deepseek-r1:14b | 14.8B | Reasoning | 46.0 | 1280 | 29.7 | 327 |
| deepseek-r1:14b | 14.8B | Planning | 52.9 | 1526 | 29.4 | 325 |
| deepseek-r1:14b | 14.8B | Language | 46.9 | 1343 | 29.3 | 481 |
| qwen3:8b | 8.2B | Reasoning | 46.4 | 2224 | 49.9 | 513 |
| qwen3:8b | 8.2B | Planning | 34.4 | 1692 | 49.9 | 376 |
| qwen3:8b | 8.2B | Language | 30.6 | 1497 | 49.7 | 401 |
| qwen3:14b | 14.8B | Reasoning | 98.1 | 2740 | 28.5 | 622 |
| qwen3:14b | 14.8B | Planning | 62.8 | 1781 | 28.6 | 402 |
| qwen3:14b | 14.8B | Language | 60.4 | 1706 | 28.5 | 427 |
| qwen3.5:9b | 9.7B | Reasoning | 107.7 | 4550 | 43.7 | 1081 |
| qwen3.5:9b | 9.7B | Planning | 72.1 | 3136 | 44.1 | 337 |
| qwen3.5:9b | 9.7B | Language | 57.9 | 2513 | 44.1 | 675 |

### Durchschnittliche Antwortzeit (alle 3 Aufgaben)

| Modell | Durchschn. Zeit (s) | Durchschn. Tok/s | Geschwindigkeits-Rang |
|--------|--------------------:|:----------------:|:---------------------:|
| mistral:7b | **14.2** | **58.0** | 1 |
| llama3.1:8b | **15.0** | **54.3** | 2 |
| qwen2.5:7b | **19.6** | **57.8** | 3 |
| deepseek-r1:7b | **25.4** | **57.7** | 4 |
| qwen3:8b | **37.1** | **49.8** | 5 |
| deepseek-r1:8b | **43.0** | **49.6** | 6 |
| deepseek-r1:14b | **48.6** | **29.5** | 7 |
| qwen3:14b | **73.8** | **28.5** | 8 |
| qwen3.5:9b | **79.2** | **44.0** | 9 |

**Anmerkung:** Modelle mit Thinking-Modus (qwen3, qwen3.5, deepseek-r1) generieren interne Denkprozesse (`<think>...</think>`), die als Tokens gezaehlt werden aber nicht im sichtbaren Output erscheinen. Das erklaert die hohe Token-Anzahl bei moderater Wortanzahl und die laengeren Antwortzeiten.

### Token-Durchsatz nach Modellgroesse

| Groessenklasse | Modelle | Tok/s Bereich |
|----------------|---------|:-------------:|
| ~7B (kein Thinking) | mistral:7b, llama3.1:8b, qwen2.5:7b | 54-59 tok/s |
| ~7-8B (mit Thinking) | deepseek-r1:7b/8b, qwen3:8b | 49-58 tok/s |
| ~10B (mit Thinking) | qwen3.5:9b | 43-44 tok/s |
| ~14-15B (mit Thinking) | deepseek-r1:14b, qwen3:14b | 28-30 tok/s |

---

## Ergebnisse: Qualitaet

Qualitaetsbewertung auf Basis der generierten Outputs (Skala 1-5):

### Reasoning (Analyse & logisches Denken)

| Modell | Struktur | Analytische Tiefe | Genauigkeit | Deutsch | Gesamt |
|--------|:--------:|:-----------------:|:-----------:|:-------:|:------:|
| mistral:7b | 3 | 3 | 3 | 4 | **3.3** |
| llama3.1:8b | 4 | 3 | 4 | 4 | **3.8** |
| qwen2.5:7b | 4 | 4 | 4 | 4 | **4.0** |
| deepseek-r1:7b | 2 | 2 | 2 | 2 | **2.0** |
| deepseek-r1:8b | 4 | 4 | 4 | 3 | **3.8** |
| deepseek-r1:14b | 4 | 4 | 4 | 4 | **4.0** |
| qwen3:8b | 4 | 4 | 4 | 4 | **4.0** |
| qwen3:14b | 5 | 5 | 5 | 5 | **5.0** |
| qwen3.5:9b | 5 | 5 | 5 | 4 | **4.8** |

### Planning (Strukturierter Tagesplan)

| Modell | Vollstaendigkeit | Detailgrad | Praxisnaehe | Deutsch | Gesamt |
|--------|:----------------:|:----------:|:-----------:|:-------:|:------:|
| mistral:7b | 3 | 3 | 3 | 4 | **3.3** |
| llama3.1:8b | 3 | 3 | 3 | 4 | **3.3** |
| qwen2.5:7b | 4 | 4 | 4 | 4 | **4.0** |
| deepseek-r1:7b | 2 | 2 | 2 | 2 | **2.0** |
| deepseek-r1:8b | 3 | 3 | 3 | 3 | **3.0** |
| deepseek-r1:14b | 4 | 3 | 4 | 4 | **3.8** |
| qwen3:8b | 4 | 4 | 4 | 4 | **4.0** |
| qwen3:14b | 5 | 4 | 5 | 5 | **4.8** |
| qwen3.5:9b | 4 | 4 | 4 | 4 | **4.0** |

### Language (Markdown-Reisebericht)

| Modell | Formatierung | Lesbarkeit | Kreativitaet | Deutsch | Gesamt |
|--------|:------------:|:----------:|:------------:|:-------:|:------:|
| mistral:7b | 3 | 3 | 2 | 4 | **3.0** |
| llama3.1:8b | 3 | 3 | 3 | 4 | **3.3** |
| qwen2.5:7b | 4 | 4 | 4 | 4 | **4.0** |
| deepseek-r1:7b | 3 | 3 | 3 | 2 | **2.8** |
| deepseek-r1:8b | 4 | 4 | 4 | 3 | **3.8** |
| deepseek-r1:14b | 4 | 4 | 4 | 4 | **4.0** |
| qwen3:8b | 4 | 4 | 3 | 4 | **3.8** |
| qwen3:14b | 5 | 5 | 5 | 5 | **5.0** |
| qwen3.5:9b | 5 | 5 | 4 | 4 | **4.5** |

### Qualitaets-Gesamtranking

| Rang | Modell | Reasoning | Planning | Language | Gesamt |
|:----:|--------|:---------:|:--------:|:--------:|:------:|
| 1 | **qwen3:14b** | 5.0 | 4.8 | 5.0 | **4.9** |
| 2 | **qwen3.5:9b** | 4.8 | 4.0 | 4.5 | **4.4** |
| 3 | **qwen2.5:7b** | 4.0 | 4.0 | 4.0 | **4.0** |
| 3 | **qwen3:8b** | 4.0 | 4.0 | 3.8 | **3.9** |
| 5 | **deepseek-r1:14b** | 4.0 | 3.8 | 4.0 | **3.9** |
| 6 | **deepseek-r1:8b** | 3.8 | 3.0 | 3.8 | **3.5** |
| 7 | **llama3.1:8b** | 3.8 | 3.3 | 3.3 | **3.5** |
| 8 | **mistral:7b** | 3.3 | 3.3 | 3.0 | **3.2** |
| 9 | **deepseek-r1:7b** | 2.0 | 2.0 | 2.8 | **2.3** |

---

## Empfehlungen fuer die Pipeline

### Beste Konfiguration nach Aufgabe

| Agent | Empfehlung (Qualitaet) | Empfehlung (Speed) | Bester Kompromiss |
|-------|----------------------|-------------------|-------------------|
| **Reasoning** | qwen3:14b (5.0, 98s) | mistral:7b (3.3, 12s) | **qwen3:8b** (4.0, 46s) |
| **Planning** | qwen3:14b (4.8, 63s) | mistral:7b (3.3, 17s) | **qwen2.5:7b** (4.0, 16s) |
| **Language** | qwen3:14b (5.0, 60s) | mistral:7b (3.0, 14s) | **qwen2.5:7b** (4.0, 21s) |

### Empfohlene Pipeline-Konfigurationen

**Maximale Qualitaet (Gesamtzeit ~220s):**
| Agent | Modell | Erwartete Zeit |
|-------|--------|:--------------:|
| Reasoning | qwen3:14b | ~98s |
| Planning | qwen3:14b | ~63s |
| Language | qwen3:14b | ~60s |

**Bester Kompromiss Qualitaet/Speed (Gesamtzeit ~80s):**
| Agent | Modell | Erwartete Zeit |
|-------|--------|:--------------:|
| Reasoning | qwen3:8b | ~46s |
| Planning | qwen2.5:7b | ~16s |
| Language | qwen2.5:7b | ~21s |

**Maximale Geschwindigkeit (Gesamtzeit ~43s):**
| Agent | Modell | Erwartete Zeit |
|-------|--------|:--------------:|
| Reasoning | llama3.1:8b | ~15s |
| Planning | mistral:7b | ~17s |
| Language | mistral:7b | ~14s |

---

## Erkenntnisse

1. **Qwen-Modelle dominieren die Qualitaet:** qwen3:14b liefert durchgehend die besten Ergebnisse, gefolgt von qwen3.5:9b. Die Qwen-Familie zeigt auch bei kleineren Modellen (qwen2.5:7b, qwen3:8b) konsistent gute Qualitaet.

2. **Thinking-Modus ist ein Qualitaetsbooster, aber teuer:** Modelle mit `<think>`-Tags (qwen3, qwen3.5, deepseek-r1) liefern deutlich bessere analytische Ergebnisse, brauchen aber 2-5x laenger wegen der internen Denkprozesse.

3. **deepseek-r1:7b ist ungeeignet:** Das kleinste DeepSeek-Reasoning-Modell produziert fehlerhafte deutsche Texte und unstrukturierte Ausgaben. Die 8b-Variante ist deutlich besser.

4. **Geschwindigkeit vs. Groesse:** Der Tok/s-Durchsatz haengt primaer von der Modellgroesse ab:
   - ~7B-Modelle: ~55-59 tok/s
   - ~14B-Modelle: ~29 tok/s (ca. halb so schnell)

5. **qwen2.5:7b ist der "Geheimtipp":** Fuer alle drei Aufgaben liefert es Qualitaet 4.0 bei voller 7B-Geschwindigkeit (~58 tok/s). Damit ist es der effizienteste Allrounder.

6. **Aktuelle Default-Konfiguration:** Die App nutzt derzeit deepseek-r1:8b fuer Reasoning, qwen2.5:7b fuer Planning und llama3.1:8b fuer Language. Basierend auf den Tests waere ein Upgrade auf qwen3:8b (Reasoning) und qwen2.5:7b (Language) sinnvoll.

---

## Rohdaten

Alle Benchmark-Ergebnisse und generierten Texte befinden sich in `benchmark/results/`:
- `summary.json` / `summary.csv` - Zusammenfassung aller Metriken
- `{modell}_{aufgabe}.txt` - Generierter Text pro Modell/Aufgabe
- `{modell}_{aufgabe}.json` - Metriken pro Modell/Aufgabe
