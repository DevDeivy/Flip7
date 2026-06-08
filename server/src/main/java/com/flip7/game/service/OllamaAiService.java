package com.flip7.game.service;

import com.flip7.game.model.Game;
import com.flip7.game.model.Player;
import com.flip7.game.model.RoundPlayer;
import com.flip7.game.repository.DeckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OllamaAiService {
    private static final String HIT = "hit";
    private static final String STAND = "stand";
    private static final String PLAY = "play";
    private static final Pattern CONTENT_PATTERN = Pattern.compile("\\\"content\\\":\\\"((?:\\\\.|[^\\\\\"])*)\\\"");
    private static final Pattern DECISION_PATTERN = Pattern.compile("\\\"decision\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern REASON_PATTERN = Pattern.compile("\\\"reason\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\\\"])*)\\\"");

    private final DeckRepository deckRepository;

    @Value("${ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${ollama.model:flip7-ai:latest}")
    private String model;

    @Value("${ollama.timeout-ms:10000}")
    private long timeoutMs;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public AiDecision decide(Game game, Player aiPlayer, RoundPlayer roundPlayer, List<Player> players) {
        int deckRemaining = deckRepository.findByGameId(game.getId())
                .map(deck -> deck.getAvailableCards().size())
                .orElse(0);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/chat"))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(game, aiPlayer, roundPlayer, players, deckRemaining), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return fallbackDecision(roundPlayer, deckRemaining, "Ollama respondió con estado " + response.statusCode());
            }

            String content = extractMessageContent(response.body());
            return parseDecision(content, roundPlayer, deckRemaining);
        } catch (HttpTimeoutException ex) {
            return fallbackDecision(roundPlayer, deckRemaining, "Tiempo de espera agotado consultando Ollama");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return fallbackDecision(roundPlayer, deckRemaining, "La consulta a Ollama fue interrumpida");
        } catch (IOException ex) {
            return fallbackDecision(roundPlayer, deckRemaining, "Fallo al consultar Ollama: " + ex.getMessage());
        } catch (Exception ex) {
            return fallbackDecision(roundPlayer, deckRemaining, "Respuesta inválida de Ollama: " + ex.getMessage());
        }
    }

    private String buildRequestBody(Game game, Player aiPlayer, RoundPlayer roundPlayer, List<Player> players, int deckRemaining) {
        String systemPrompt = escapeJson(buildSystemPrompt());
        String userPrompt = escapeJson(buildUserPrompt(game, aiPlayer, roundPlayer, players, deckRemaining));

        return "{" 
                + "\"model\":\"" + escapeJson(model) + "\"," 
                + "\"stream\":false,"
            + "\"keep_alive\":\"10m\","
                + "\"format\":\"json\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + systemPrompt + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + userPrompt + "\"}"
                + "]}";
    }

    private String buildSystemPrompt() {
        return "Eres la IA de Flip7 en una partida 1 vs 1. "
                + "Debes decidir la jugada del jugador IA usando solo el estado actual. "
                + "Responde siempre con JSON válido y nada más. "
                + "Formato exacto: {\"decision\":\"hit|stand|play\",\"reason\":\"explicacion corta\"}. "
                + "No uses markdown ni texto extra. "
                + "Si no tienes cartas, elige hit. "
                + "Usa play solo como sinónimo de hit.";
    }

    private String buildUserPrompt(Game game, Player aiPlayer, RoundPlayer roundPlayer, List<Player> players, int deckRemaining) {
        StringBuilder opponentSummary = new StringBuilder();
        for (Player player : players) {
            if (!player.getId().equals(aiPlayer.getId())) {
                if (opponentSummary.length() > 0) {
                    opponentSummary.append("; ");
                }
                opponentSummary.append(player.getName())
                        .append(" puntosTotales=")
                        .append(player.getTotalPoints());
            }
        }

        return "Estado de la partida Flip7:\n"
                + "Ronda: " + game.getCurrentRound() + "\n"
                + "Jugador IA: " + aiPlayer.getName() + "\n"
                + "Cartas actuales: " + roundPlayer.getCurrentCards() + "\n"
                + "Puntos de ronda: " + roundPlayer.getRoundPoints() + "\n"
                + "Puntos totales: " + aiPlayer.getTotalPoints() + "\n"
                + "Segunda oportunidad: " + roundPlayer.isHasSecondChance() + "\n"
                + "Estado: " + roundPlayer.getStatus().name() + "\n"
                + "Oponente: " + opponentSummary + "\n"
                + "Mazo restante: " + deckRemaining + "\n"
                + "Decide ahora si el jugador IA debe hacer hit o stand.";
    }

    private String extractMessageContent(String responseBody) {
        String rawContent = matchGroup(CONTENT_PATTERN, responseBody);
        return unescapeJsonString(rawContent == null ? "" : rawContent);
    }

    private AiDecision parseDecision(String content, RoundPlayer roundPlayer, int deckRemaining) {
        String decision = normalizeDecision(matchGroup(DECISION_PATTERN, content));
        String reason = unescapeJsonString(matchGroup(REASON_PATTERN, content));

        if (!isAllowedDecision(decision)) {
            return fallbackDecision(roundPlayer, deckRemaining, "Decisión no permitida por Ollama");
        }

        if (reason == null || reason.isBlank()) {
            reason = "Decisión válida generada por Ollama.";
        }

        return new AiDecision(decision, reason);
    }

    private boolean isAllowedDecision(String decision) {
        return HIT.equals(decision) || STAND.equals(decision) || PLAY.equals(decision);
    }

    private String normalizeDecision(String decision) {
        if (decision == null) {
            return HIT;
        }

        return switch (decision.trim().toLowerCase()) {
            case STAND -> STAND;
            case PLAY -> HIT;
            case HIT -> HIT;
            default -> HIT;
        };
    }

    private AiDecision fallbackDecision(RoundPlayer roundPlayer, int deckRemaining, String reason) {
        int score = roundPlayer.getRoundPoints();
        if (score < 1) {
            score = roundPlayer.getCurrentCards().stream()
                    .filter(card -> card >= 0 && card <= 12)
                    .mapToInt(Integer::intValue)
                    .sum();
        }

        String decision = score >= 18 || deckRemaining == 0 ? STAND : HIT;
        return new AiDecision(decision, reason);
    }

    private String matchGroup(Pattern pattern, String input) {
        if (input == null) {
            return null;
        }

        Matcher matcher = pattern.matcher(input);
        if (!matcher.find()) {
            return null;
        }

        return matcher.group(1);
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJsonString(String value) {
        if (value == null) {
            return null;
        }

        return value
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    public record AiDecision(String decision, String reason) {
    }
}