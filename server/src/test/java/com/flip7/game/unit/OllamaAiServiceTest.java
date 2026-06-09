package com.flip7.game.unit;

import com.flip7.game.model.Deck;
import com.flip7.game.model.Game;
import com.flip7.game.model.Player;
import com.flip7.game.model.RoundPlayer;
import com.flip7.game.repository.DeckRepository;
import com.flip7.game.service.OllamaAiService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OllamaAiServiceTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void decide_returnsStandForValidOllamaResponse() throws Exception {
        DeckRepository deckRepository = mock(DeckRepository.class);
        Deck deck = new Deck();
        deck.reset();
        when(deckRepository.findByGameId(1L)).thenReturn(Optional.of(deck));

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/chat", this::respondWithValidStandDecision);
        server.start();

        OllamaAiService service = new OllamaAiService(deckRepository);
        setField(service, "baseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
        setField(service, "model", "test-model");
        setField(service, "timeoutMs", 2000L);

        OllamaAiService.AiDecision decision = service.decide(game(), aiPlayer(), roundPlayer(List.of(3, 5), 8), List.of(aiPlayer(), opponent()));

        assertThat(decision.decision()).isEqualTo("stand");
        assertThat(decision.reason()).contains("segura");
    }

    @Test
    void decide_mapsPlayDecisionToHit() throws Exception {
        DeckRepository deckRepository = mock(DeckRepository.class);
        Deck deck = new Deck();
        deck.reset();
        when(deckRepository.findByGameId(1L)).thenReturn(Optional.of(deck));

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/chat", this::respondWithPlayDecision);
        server.start();

        OllamaAiService service = new OllamaAiService(deckRepository);
        setField(service, "baseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
        setField(service, "model", "test-model");
        setField(service, "timeoutMs", 2000L);

        OllamaAiService.AiDecision decision = service.decide(game(), aiPlayer(), roundPlayer(List.of(1), 1), List.of(aiPlayer(), opponent()));

        assertThat(decision.decision()).isEqualTo("hit");
    }

    @Test
    void decide_fallsBackToStandWhenServiceIsUnavailableAndDeckIsEmpty() throws Exception {
        DeckRepository deckRepository = mock(DeckRepository.class);
        when(deckRepository.findByGameId(1L)).thenReturn(Optional.empty());

        OllamaAiService service = new OllamaAiService(deckRepository);
        setField(service, "baseUrl", "http://127.0.0.1:1");
        setField(service, "model", "test-model");
        setField(service, "timeoutMs", 200L);

        OllamaAiService.AiDecision decision = service.decide(game(), aiPlayer(), roundPlayer(List.of(2, 3), 5), List.of(aiPlayer(), opponent()));

        assertThat(decision.decision()).isEqualTo("stand");
        assertThat(decision.reason()).contains("Fallo al consultar Ollama");
    }

    @Test
    void decide_fallsBackWhenOllamaReturnsInvalidDecision() throws Exception {
        DeckRepository deckRepository = mock(DeckRepository.class);
        Deck deck = new Deck();
        deck.reset();
        when(deckRepository.findByGameId(1L)).thenReturn(Optional.of(deck));

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/chat", this::respondWithInvalidDecision);
        server.start();

        OllamaAiService service = new OllamaAiService(deckRepository);
        setField(service, "baseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
        setField(service, "model", "test-model");
        setField(service, "timeoutMs", 2000L);

        OllamaAiService.AiDecision decision = service.decide(game(), aiPlayer(), roundPlayer(List.of(9, 9), 18), List.of(aiPlayer(), opponent()));

        assertThat(decision.decision()).isEqualTo("hit");
        assertThat(decision.reason()).contains("???");
    }

    @Test
    void decide_usesFallbackWhenHttpStatusIsNotSuccessful() throws Exception {
        DeckRepository deckRepository = mock(DeckRepository.class);
        when(deckRepository.findByGameId(1L)).thenReturn(Optional.empty());

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/chat", this::respondWithServerError);
        server.start();

        OllamaAiService service = new OllamaAiService(deckRepository);
        setField(service, "baseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
        setField(service, "model", "test-model");
        setField(service, "timeoutMs", 2000L);

        OllamaAiService.AiDecision decision = service.decide(game(), aiPlayer(), roundPlayer(List.of(9, 9), 18), List.of(aiPlayer(), opponent()));

        assertThat(decision.reason()).contains("estado 500");
    }

    @Test
    void decide_fillsDefaultReasonWhenOllamaSendsBlankReason() throws Exception {
        DeckRepository deckRepository = mock(DeckRepository.class);
        Deck deck = new Deck();
        deck.reset();
        when(deckRepository.findByGameId(1L)).thenReturn(Optional.of(deck));

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/chat", this::respondWithBlankReason);
        server.start();

        OllamaAiService service = new OllamaAiService(deckRepository);
        setField(service, "baseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
        setField(service, "model", "test-model");
        setField(service, "timeoutMs", 2000L);

        OllamaAiService.AiDecision decision = service.decide(game(), aiPlayer(), roundPlayer(List.of(1), 1), List.of(aiPlayer(), opponent()));

        assertThat(decision.decision()).isEqualTo("stand");
        assertThat(decision.reason()).contains("Decisión válida generada");
    }

    @Test
    void helperMethods_coverDecisionNormalizationAndMatchingBranches() throws Exception {
        OllamaAiService service = new OllamaAiService(mock(DeckRepository.class));

        assertThat((String) invoke(service, "normalizeDecision", new Class[]{String.class}, (Object) null)).isEqualTo("hit");
        assertThat((String) invoke(service, "normalizeDecision", new Class[]{String.class}, "stand")).isEqualTo("stand");
        assertThat((String) invoke(service, "normalizeDecision", new Class[]{String.class}, "play")).isEqualTo("hit");
        assertThat((String) invoke(service, "normalizeDecision", new Class[]{String.class}, "other")).isEqualTo("hit");

        assertThat((Boolean) invoke(service, "isAllowedDecision", new Class[]{String.class}, "hit")).isTrue();
        assertThat((Boolean) invoke(service, "isAllowedDecision", new Class[]{String.class}, "stand")).isTrue();
        assertThat((Boolean) invoke(service, "isAllowedDecision", new Class[]{String.class}, "play")).isTrue();
        assertThat((Boolean) invoke(service, "isAllowedDecision", new Class[]{String.class}, "jump")).isFalse();

        @SuppressWarnings("unchecked")
        Class<Pattern> patternClass = (Class<Pattern>) Class.forName("java.util.regex.Pattern");
        Pattern decisionPattern = (Pattern) getStaticField(OllamaAiService.class, "DECISION_PATTERN");
        assertThat((String) invoke(service, "matchGroup", new Class[]{patternClass, String.class}, decisionPattern, null)).isNull();
        assertThat((String) invoke(service, "matchGroup", new Class[]{patternClass, String.class}, decisionPattern, "{}")).isNull();
        assertThat((String) invoke(service, "matchGroup", new Class[]{patternClass, String.class}, decisionPattern, "{\"decision\":\"hit\"}")).isEqualTo("hit");
    }

    @Test
    void helperMethods_coverFallbackEscapeAndUnescapeBranches() throws Exception {
        OllamaAiService service = new OllamaAiService(mock(DeckRepository.class));

        RoundPlayer lowScore = roundPlayer(List.of(2, 3), 0);
        OllamaAiService.AiDecision hitDecision = (OllamaAiService.AiDecision) invoke(
                service,
                "fallbackDecision",
                new Class[]{RoundPlayer.class, int.class, String.class},
                lowScore,
                5,
                "fallback"
        );
        assertThat(hitDecision.decision()).isEqualTo("hit");

        RoundPlayer highScoreFromCards = roundPlayer(List.of(9, 9), 0);
        OllamaAiService.AiDecision standByScore = (OllamaAiService.AiDecision) invoke(
                service,
                "fallbackDecision",
                new Class[]{RoundPlayer.class, int.class, String.class},
                highScoreFromCards,
                5,
                "fallback"
        );
        assertThat(standByScore.decision()).isEqualTo("stand");

        RoundPlayer emptyDeck = roundPlayer(List.of(1), 1);
        OllamaAiService.AiDecision standByDeck = (OllamaAiService.AiDecision) invoke(
                service,
                "fallbackDecision",
                new Class[]{RoundPlayer.class, int.class, String.class},
                emptyDeck,
                0,
                "fallback"
        );
        assertThat(standByDeck.decision()).isEqualTo("stand");

        assertThat((String) invoke(service, "escapeJson", new Class[]{String.class}, (Object) null)).isEqualTo("");
        assertThat((String) invoke(service, "escapeJson", new Class[]{String.class}, "a\n\t\"b\\c")).isEqualTo("a\\n\\t\\\"b\\\\c");

        assertThat((String) invoke(service, "unescapeJsonString", new Class[]{String.class}, (Object) null)).isNull();
        assertThat((String) invoke(service, "unescapeJsonString", new Class[]{String.class}, "line\\ntext\\t\\\"q\\\"\\\\")).isEqualTo("line\ntext\t\"q\"\\");
    }

    private void respondWithValidStandDecision(HttpExchange exchange) throws IOException {
        String body = "{\"message\":{\"content\":\"{\\\"decision\\\":\\\"stand\\\",\\\"reason\\\":\\\"jugada segura\\\"}\"}}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private void respondWithPlayDecision(HttpExchange exchange) throws IOException {
        String body = "{\"message\":{\"content\":\"{\\\"decision\\\":\\\"play\\\",\\\"reason\\\":\\\"seguir\\\"}\"}}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private void respondWithInvalidDecision(HttpExchange exchange) throws IOException {
        String body = "{\"message\":{\"content\":\"{\\\"decision\\\":\\\"jump\\\",\\\"reason\\\":\\\"???\\\"}\"}}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private void respondWithServerError(HttpExchange exchange) throws IOException {
        byte[] bytes = "oops".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(500, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private void respondWithBlankReason(HttpExchange exchange) throws IOException {
        String body = "{\"message\":{\"content\":\"{\\\"decision\\\":\\\"stand\\\",\\\"reason\\\":\\\"   \\\"}\"}}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private Game game() {
        Game game = new Game();
        game.setId(1L);
        game.setCurrentRound(1);
        return game;
    }

    private Player aiPlayer() {
        Player player = new Player();
        player.setId(10L);
        player.setName("FLIP7 AI");
        player.setTotalPoints(20);
        player.setAiControlled(true);
        return player;
    }

    private Player opponent() {
        Player player = new Player();
        player.setId(11L);
        player.setName("User");
        player.setTotalPoints(30);
        return player;
    }

    private RoundPlayer roundPlayer(List<Integer> cards, int roundPoints) {
        RoundPlayer roundPlayer = new RoundPlayer();
        roundPlayer.setCurrentCards(cards);
        roundPlayer.setRoundPoints(roundPoints);
        return roundPlayer;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object getStaticField(Class<?> type, String fieldName) throws Exception {
        Field field = type.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(null);
    }

    private Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }
}
