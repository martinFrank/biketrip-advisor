package de.biketrip.advisor.agent;

import de.biketrip.advisor.config.LangChainConfig;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration-Tests die gegen ein laufendes Ollama testen.
 * <p>
 * Ausführen mit: {@code mvn test -Dgroups=integration}
 * <p>
 * Voraussetzung: Ollama läuft auf localhost:11434 mit den benötigten Modellen.
 * Diese Tests starten KEINEN Spring-Kontext — sie bauen die Models direkt auf.
 * So kann man schnell Prompts iterieren ohne die ganze Applikation neu zu starten.
 */
@Tag("integration")
class AgentIntegrationTest {

    private static final String OLLAMA_BASE_URL = "http://localhost:11434";

    @BeforeAll
    static void checkOllamaAvailable() {
        try {
            ChatLanguageModel model = LangChainConfig.buildModel(OLLAMA_BASE_URL, "mistral", 0.7);
            model.generate(UserMessage.from("ping"));
        } catch (Exception e) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false,
                    "Ollama nicht erreichbar auf " + OLLAMA_BASE_URL + ": " + e.getMessage());
        }
    }

    @Test
    void chatAgentPromptProducesStructuredOutput() {
        ChatLanguageModel model = LangChainConfig.buildModel(OLLAMA_BASE_URL, "mistral", 0.7);

        String systemPrompt = """
                Du bist ein erfahrener Fahrradtouren-Berater. Deine Aufgabe ist es, die Anfrage des Nutzers \
                zu verstehen und eine klare, strukturierte Zusammenfassung zu erstellen.

                Extrahiere folgende Informationen:
                - Start- und Zielort
                - Anzahl der Tage
                - Budgetlimit
                - Besondere Wünsche (Sightseeing, Schwierigkeitsgrad, etc.)

                Antworte auf Deutsch. Gib eine strukturierte Zusammenfassung aus, keine Prosa.
                """;

        Response<AiMessage> response = model.generate(
                SystemMessage.from(systemPrompt),
                UserMessage.from("Ich möchte eine 3-tägige Radtour von Freiburg nach Basel, Budget 500€, gerne Weinberge")
        );

        String output = response.content().text();
        System.out.println("=== ChatAgent Output ===");
        System.out.println(output);
        System.out.println("========================");

        assertThat(output).isNotBlank();
        assertThat(output.toLowerCase()).containsAnyOf("freiburg", "basel");
    }

    @Test
    void reasoningAgentPromptAnalyzesFeasibility() {
        ChatLanguageModel model = LangChainConfig.buildModel(OLLAMA_BASE_URL, "deepseek-r1:8b", 0.2);

        String systemPrompt = """
                Du bist ein analytischer Experte für Radtouren-Planung. Analysiere die folgende \
                Touranfrage gründlich:

                1. MACHBARKEIT: Ist die Strecke in der angegebenen Zeit realistisch?
                2. RISIKEN: Wetter, Höhenprofil, Verkehr
                3. BUDGET-CHECK: Ist das Budget realistisch?

                Denke Schritt für Schritt. Sei kritisch und ehrlich.
                Antworte auf Deutsch.
                """;

        Response<AiMessage> response = model.generate(
                SystemMessage.from(systemPrompt),
                UserMessage.from("3 Tage Freiburg → Basel, 500€ Budget, Weinberge")
        );

        String output = response.content().text();
        System.out.println("=== ReasoningAgent Output ===");
        System.out.println(output);
        System.out.println("=============================");

        assertThat(output).isNotBlank();
    }

    @Test
    void planningAgentPromptCreatesStructuredPlan() {
        ChatLanguageModel model = LangChainConfig.buildModel(OLLAMA_BASE_URL, "qwen2.5:7b", 0.4);

        String systemPrompt = """
                Du bist ein Radtouren-Planer. Erstelle einen detaillierten Tagesplan:

                Für jeden Tag:
                - Tag X: [Startort] → [Zielort]
                - Distanz: XX km
                - Höhenprofil: [flach/hügelig/bergig]
                - Unterkunft: [Vorschlag]

                Antworte auf Deutsch.
                """;

        Response<AiMessage> response = model.generate(
                SystemMessage.from(systemPrompt),
                UserMessage.from("3 Tage Freiburg → Basel, machbar, ca. 60km/Tag")
        );

        String output = response.content().text();
        System.out.println("=== PlanningAgent Output ===");
        System.out.println(output);
        System.out.println("============================");

        assertThat(output).isNotBlank();
        assertThat(output).containsIgnoringCase("Tag");
    }

    @Test
    void languageAgentPromptProducesMarkdown() {
        ChatLanguageModel model = LangChainConfig.buildModel(OLLAMA_BASE_URL, "llama3.1:8b", 0.8);

        String systemPrompt = """
                Du bist ein professioneller Reise-Redakteur. Verwandle den folgenden Tourenplan \
                in einen ansprechenden Markdown-Reisebericht.

                Verwende Markdown mit Überschriften (##, ###), Tabellen, und Emoji.
                Antworte auf Deutsch. Nur Markdown-Ausgabe.
                """;

        Response<AiMessage> response = model.generate(
                SystemMessage.from(systemPrompt),
                UserMessage.from("Tag 1: Freiburg → Breisach (40km, flach)\nTag 2: Breisach → Mulhouse (50km, hügelig)\nTag 3: Mulhouse → Basel (35km, flach)")
        );

        String output = response.content().text();
        System.out.println("=== LanguageAgent Output ===");
        System.out.println(output);
        System.out.println("============================");

        assertThat(output).isNotBlank();
        assertThat(output).contains("#");
    }

    @Test
    void fullPipelineSimulation() {
        // Simuliert die gesamte Pipeline sequentiell gegen echte Modelle.
        // Nützlich um Prompt-Änderungen end-to-end zu testen.
        String userRequest = "Ich plane eine 2-tägige Radtour am Bodensee. Budget: 300€. Gemütliches Tempo.";

        // Stage 1: Chat
        ChatLanguageModel chatModel = LangChainConfig.buildModel(OLLAMA_BASE_URL, "mistral", 0.7);
        Response<AiMessage> chatResponse = chatModel.generate(
                SystemMessage.from("Du bist ein Fahrradtouren-Berater. Erstelle eine strukturierte Zusammenfassung der Anfrage. Antworte auf Deutsch."),
                UserMessage.from(userRequest)
        );
        String chatOutput = chatResponse.content().text();
        System.out.println("=== Stage 1: Chat ===\n" + chatOutput + "\n");

        // Stage 2: Reasoning
        ChatLanguageModel reasonModel = LangChainConfig.buildModel(OLLAMA_BASE_URL, "deepseek-r1:8b", 0.2);
        Response<AiMessage> reasonResponse = reasonModel.generate(
                SystemMessage.from("Du bist ein analytischer Experte. Analysiere Machbarkeit, Risiken und Budget. Antworte auf Deutsch."),
                UserMessage.from(chatOutput)
        );
        String reasonOutput = reasonResponse.content().text();
        System.out.println("=== Stage 2: Reasoning ===\n" + reasonOutput + "\n");

        // Stage 3: Planning
        ChatLanguageModel planModel = LangChainConfig.buildModel(OLLAMA_BASE_URL, "qwen2.5:7b", 0.4);
        Response<AiMessage> planResponse = planModel.generate(
                SystemMessage.from("Du bist ein Radtouren-Planer. Erstelle einen detaillierten Tagesplan. Antworte auf Deutsch."),
                UserMessage.from(reasonOutput)
        );
        String planOutput = planResponse.content().text();
        System.out.println("=== Stage 3: Planning ===\n" + planOutput + "\n");

        // Stage 4: Language
        ChatLanguageModel langModel = LangChainConfig.buildModel(OLLAMA_BASE_URL, "llama3.1:8b", 0.8);
        Response<AiMessage> langResponse = langModel.generate(
                SystemMessage.from("Verwandle in einen Markdown-Reisebericht. Antworte auf Deutsch."),
                UserMessage.from(planOutput)
        );
        String langOutput = langResponse.content().text();
        System.out.println("=== Stage 4: Language ===\n" + langOutput + "\n");

        assertThat(langOutput).isNotBlank();
    }
}
