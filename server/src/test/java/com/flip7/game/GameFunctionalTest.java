package com.flip7.game;

import com.flip7.game.model.Deck;
import com.flip7.game.repository.DeckRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GameFunctionalTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private DeckRepository deckRepository;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private void configureDeck(Long gameId, int... cards) {
        Deck deck = deckRepository.findByGameId(gameId).orElseThrow();
        zeroDeck(deck);
        for (int card : cards) {
            int current = getCount(deck, card);
            setCount(deck, card, current + 1);
        }
        deckRepository.save(deck);
    }

    private void zeroDeck(Deck deck) {
        for (int i = 0; i <= 12; i++) setCount(deck, i, 0);
        setCount(deck, 100, 0);
        setCount(deck, 101, 0);
        setCount(deck, 102, 0);
        setCount(deck, 200, 0);
        setCount(deck, 201, 0);
        setCount(deck, 202, 0);
        setCount(deck, 203, 0);
        setCount(deck, 204, 0);
        setCount(deck, 205, 0);
    }

    private int getCount(Deck deck, int card) {
        return switch (card) {
            case 0 -> deck.getCount0();
            case 1 -> deck.getCount1();
            case 2 -> deck.getCount2();
            case 3 -> deck.getCount3();
            case 4 -> deck.getCount4();
            case 5 -> deck.getCount5();
            case 6 -> deck.getCount6();
            case 7 -> deck.getCount7();
            case 8 -> deck.getCount8();
            case 9 -> deck.getCount9();
            case 10 -> deck.getCount10();
            case 11 -> deck.getCount11();
            case 12 -> deck.getCount12();
            case 100 -> deck.getCountFreeze();
            case 101 -> deck.getCountFlipThree();
            case 102 -> deck.getCountSecondChance();
            case 200 -> deck.getCountX2();
            case 201 -> deck.getCountPlus2();
            case 202 -> deck.getCountPlus4();
            case 203 -> deck.getCountPlus6();
            case 204 -> deck.getCountPlus8();
            case 205 -> deck.getCountPlus10();
            default -> throw new IllegalArgumentException("Unknown card: " + card);
        };
    }

    private void setCount(Deck deck, int card, int count) {
        switch (card) {
            case 0 -> deck.setCount0(count);
            case 1 -> deck.setCount1(count);
            case 2 -> deck.setCount2(count);
            case 3 -> deck.setCount3(count);
            case 4 -> deck.setCount4(count);
            case 5 -> deck.setCount5(count);
            case 6 -> deck.setCount6(count);
            case 7 -> deck.setCount7(count);
            case 8 -> deck.setCount8(count);
            case 9 -> deck.setCount9(count);
            case 10 -> deck.setCount10(count);
            case 11 -> deck.setCount11(count);
            case 12 -> deck.setCount12(count);
            case 100 -> deck.setCountFreeze(count);
            case 101 -> deck.setCountFlipThree(count);
            case 102 -> deck.setCountSecondChance(count);
            case 200 -> deck.setCountX2(count);
            case 201 -> deck.setCountPlus2(count);
            case 202 -> deck.setCountPlus4(count);
            case 203 -> deck.setCountPlus6(count);
            case 204 -> deck.setCountPlus8(count);
            case 205 -> deck.setCountPlus10(count);
        }
    }

    private Long createGame(String... players) throws Exception {
        String body = mapper.writeValueAsString(Map.of("players", List.of(players)));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/api/flip/game"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        assertThat(res.statusCode()).isEqualTo(200);
        return mapper.readTree(res.body()).get("gameId").asLong();
    }

    private HttpResponse<String> drawCard(Long gameId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/api/flip/game/" + gameId + "/draw"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> stand(Long gameId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/api/flip/game/" + gameId + "/stand"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void createGame_withValidPlayers_returnsGameId() throws Exception {
        String body = mapper.writeValueAsString(Map.of("players", List.of("Alice", "Bob", "Charlie", "Diana")));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/api/flip/game"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());

        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(res.body()).get("gameId")).isNotNull();
    }

    @Test
    void createGame_with3Players_returnsBadRequest() throws Exception {
        String body = mapper.writeValueAsString(Map.of("players", List.of("A", "B", "C")));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/api/flip/game"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());

        assertThat(res.statusCode()).isEqualTo(400);
    }

    @Test
    void createGame_with9Players_returnsBadRequest() throws Exception {
        String body = mapper.writeValueAsString(Map.of("players", List.of("A", "B", "C", "D", "E", "F", "G", "H", "I")));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/api/flip/game"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());

        assertThat(res.statusCode()).isEqualTo(400);
    }

    @Test
    void getGame_withExistingId_returnsGameState() throws Exception {
        Long gameId = createGame("Alice", "Bob", "Charlie", "Diana");

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/api/flip/game/" + gameId))
                .GET()
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());

        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(res.body()).get("gameId").asLong()).isEqualTo(gameId);
    }

    @Test
    void drawCard_returnsOk() throws Exception {
        Long gameId = createGame("Alice", "Bob", "Charlie", "Diana");

        HttpResponse<String> res = drawCard(gameId);
        assertThat(res.statusCode()).isEqualTo(200);
    }

    @Test
    void stand_afterDraw_returnsOk() throws Exception {
        Long gameId = createGame("Alice", "Bob", "Charlie", "Diana");

        configureDeck(gameId, 4);
        drawCard(gameId);
        configureDeck(gameId, 9);
        drawCard(gameId);
        configureDeck(gameId, 9);
        drawCard(gameId);
        configureDeck(gameId, 9);
        drawCard(gameId);

        HttpResponse<String> res = stand(gameId);
        assertThat(res.statusCode()).isEqualTo(200);
    }

    @Test
    void fullRound_allStand_roundAdvances() throws Exception {
        Long gameId = createGame("Alice", "Bob", "Charlie", "Diana");

        configureDeck(gameId, 5);
        drawCard(gameId);
        configureDeck(gameId, 3);
        drawCard(gameId);
        configureDeck(gameId, 7);
        drawCard(gameId);
        configureDeck(gameId, 9);
        drawCard(gameId);

        stand(gameId);
        stand(gameId);
        stand(gameId);

        HttpResponse<String> res = stand(gameId);
        assertThat(res.statusCode()).isEqualTo(200);
    }

}
