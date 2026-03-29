import { writeFileSync, mkdirSync } from 'fs';
import { join } from 'path';

const OLLAMA_HOST = 'http://192.168.0.251:11434';
const OUTPUT_DIR = 'benchmark/results';
mkdirSync(OUTPUT_DIR, { recursive: true });

const MODELS = [
  'mistral:7b',
  'llama3.1:8b',
  'qwen2.5:7b',
  'deepseek-r1:7b',
  'deepseek-r1:8b',
  'deepseek-r1:14b',
  'qwen3:8b',
  'qwen3:14b',
  'qwen3.5:9b',
];

// --- Fixed test inputs ---

const REASONING_INPUT = `## Zusammenfassung der Touranfrage

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
- Sehenswürdigkeiten: Nürnberger Altstadt, Amberg (historische Altstadt), Kallmünz (Künstlerdorf), Regensburger Dom und Steinerne Brücke`;

const PLANNING_INPUT = `## Analyse der Radtour Nürnberg → Regensburg

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
- Unterkünfte vorab reservieren (besonders in Kallmünz begrenzt)`;

const LANGUAGE_INPUT = `## Detaillierter Tourenplan: Nürnberg → Regensburg (3 Tage)

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
- Fahrradwerkstätten in Amberg und Regensburg`;

// System prompts
const TASKS = {
  reasoning: {
    system: `Du bist ein analytischer Experte für Radtouren-Planung. Analysiere die folgende Touranfrage gründlich:

1. MACHBARKEIT: Ist die Strecke in der angegebenen Zeit realistisch? Berechne Tagesetappen (ca. 60-80 km/Tag für Tourenradfahrer).
2. RISIKEN: Wetter, Höhenprofil, Verkehr, Grenzübergänge
3. BUDGET-CHECK: Ist das Budget realistisch? (Unterkunft ~40-80€/Nacht, Verpflegung ~20-30€/Tag)
4. ANFORDERUNGEN: Liste die konkreten Anforderungen strukturiert auf.

Denke Schritt für Schritt. Sei kritisch und ehrlich bei der Bewertung.
Antworte auf Deutsch.`,
    input: REASONING_INPUT,
  },
  planning: {
    system: `Du bist ein Radtouren-Planer. Erstelle basierend auf der Analyse einen detaillierten Tagesplan im folgenden Format:

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

Antworte auf Deutsch. Verwende klare Struktur mit Aufzählungszeichen.`,
    input: PLANNING_INPUT,
  },
  language: {
    system: `Du bist ein professioneller Reise-Redakteur. Verwandle den folgenden Tourenplan in einen ansprechenden, gut formatierten Markdown-Reisebericht.

Anforderungen:
- Verwende Markdown mit Überschriften (##, ###), Tabellen, und Emoji wo passend
- Schreibe einen einladenden Einleitungstext
- Formatiere die Tagesetappen als ansprechende Abschnitte
- Erstelle eine Kostenübersicht als Markdown-Tabelle
- Füge einen motivierenden Schlusssatz hinzu
- Der Bericht soll sich wie ein professioneller Reiseführer lesen

Antworte auf Deutsch. Nur Markdown-Ausgabe, kein Kommentar.`,
    input: LANGUAGE_INPUT,
  },
};

async function runTest(model, taskName, task) {
  const payload = {
    model,
    messages: [
      { role: 'system', content: task.system },
      { role: 'user', content: task.input },
    ],
    stream: false,
    options: { temperature: 0.7 },
  };

  const startTime = Date.now();

  const response = await fetch(`${OLLAMA_HOST}/api/chat`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
    signal: AbortSignal.timeout(300000), // 5 min timeout
  });

  const data = await response.json();
  const wallTimeMs = Date.now() - startTime;

  const content = data.message?.content || 'ERROR';
  const evalCount = data.eval_count || 0;
  const evalDuration = data.eval_duration || 0;
  const promptEvalCount = data.prompt_eval_count || 0;
  const totalDuration = data.total_duration || 0;

  const tokensPerSec = evalDuration > 0 ? (evalCount / (evalDuration / 1e9)).toFixed(1) : '0';
  const totalDurationS = (totalDuration / 1e9).toFixed(1);
  const wallTimeS = (wallTimeMs / 1000).toFixed(1);
  const wordCount = content.split(/\s+/).filter(Boolean).length;

  const safeModel = model.replace(/[:/]/g, '_');

  // Save output text
  writeFileSync(join(OUTPUT_DIR, `${safeModel}_${taskName}.txt`), content, 'utf8');

  const result = {
    model,
    task: taskName,
    wall_time_s: parseFloat(wallTimeS),
    total_duration_s: parseFloat(totalDurationS),
    output_tokens: evalCount,
    tokens_per_sec: parseFloat(tokensPerSec),
    prompt_tokens: promptEvalCount,
    word_count: wordCount,
  };

  // Save metrics JSON
  writeFileSync(join(OUTPUT_DIR, `${safeModel}_${taskName}.json`), JSON.stringify(result, null, 2), 'utf8');

  return result;
}

async function main() {
  console.log('========================================');
  console.log('  Biketrip Advisor LLM Benchmark');
  console.log(`  ${new Date().toLocaleString()}`);
  console.log(`  Host: ${OLLAMA_HOST}`);
  console.log(`  Models: ${MODELS.length} | Tasks: ${Object.keys(TASKS).length}`);
  console.log('========================================\n');

  const allResults = [];
  const totalTests = MODELS.length * Object.keys(TASKS).length;
  let current = 0;

  for (const model of MODELS) {
    console.log(`\n### ${model} ###`);

    for (const [taskName, task] of Object.entries(TASKS)) {
      current++;
      process.stdout.write(`[${current}/${totalTests}] ${taskName}... `);

      try {
        const result = await runTest(model, taskName, task);
        allResults.push(result);
        console.log(`${result.wall_time_s}s | ${result.output_tokens} tokens | ${result.tokens_per_sec} tok/s | ${result.word_count} words`);
      } catch (err) {
        console.log(`ERROR: ${err.message}`);
        allResults.push({
          model, task: taskName,
          wall_time_s: 0, total_duration_s: 0, output_tokens: 0,
          tokens_per_sec: 0, prompt_tokens: 0, word_count: 0, error: err.message,
        });
      }
    }
  }

  // Write summary CSV
  const csvHeader = 'model,task,wall_time_s,total_duration_s,output_tokens,tokens_per_sec,prompt_tokens,word_count';
  const csvRows = allResults.map(r =>
    `"${r.model}","${r.task}",${r.wall_time_s},${r.total_duration_s},${r.output_tokens},${r.tokens_per_sec},${r.prompt_tokens},${r.word_count}`
  );
  writeFileSync(join(OUTPUT_DIR, 'summary.csv'), [csvHeader, ...csvRows].join('\n'), 'utf8');

  // Write summary JSON
  writeFileSync(join(OUTPUT_DIR, 'summary.json'), JSON.stringify(allResults, null, 2), 'utf8');

  console.log('\n========================================');
  console.log('  Benchmark complete!');
  console.log(`  Results: ${OUTPUT_DIR}/summary.csv`);
  console.log('========================================');

  // Print summary table
  console.log('\n--- SUMMARY TABLE ---\n');
  console.log('Model                | Task       | Time(s) | Tokens | Tok/s  | Words');
  console.log('---------------------|------------|---------|--------|--------|------');
  for (const r of allResults) {
    const m = r.model.padEnd(20);
    const t = r.task.padEnd(10);
    const time = String(r.wall_time_s).padStart(7);
    const tok = String(r.output_tokens).padStart(6);
    const tps = String(r.tokens_per_sec).padStart(6);
    const w = String(r.word_count).padStart(5);
    console.log(`${m} | ${t} | ${time} | ${tok} | ${tps} | ${w}`);
  }
}

main().catch(console.error);
