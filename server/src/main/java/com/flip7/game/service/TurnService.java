package com.flip7.game.service;

import com.flip7.game.GameStatus;
import com.flip7.game.RoundPlayerStatus;
import com.flip7.game.model.Game;
import com.flip7.game.model.Player;
import com.flip7.game.model.RoundPlayer;
import com.flip7.game.repository.GameRepository;
import com.flip7.game.repository.PlayerRepository;
import com.flip7.game.repository.RoundPlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PreDestroy;

@Service
@RequiredArgsConstructor
public class TurnService {

    private static final int POINTS_TO_WIN = 200;
    private static final int FLIP7_BONUS = 15;
    private static final int FLIP7_COUNT = 7;
    private static final int FREEZE = 100;
    private static final int FLIP_THREE = 101;
    private static final int SECOND_CHANCE = 102;
    private static final int X2 = 200;

    private final RoundPlayerRepository roundPlayerRepository;
    private final PlayerRepository playerRepository;
    private final GameRepository gameRepository;
    private final DeckService deckService;
    private final OllamaAiService ollamaAiService;
    private final TransactionTemplate transactionTemplate;

    @Value("${game.ai-turn-delay-ms:1200}")
    private long aiTurnDelayMs;

    private final ScheduledExecutorService aiTurnExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "flip7-ai-turns");
        thread.setDaemon(true);
        return thread;
    });

    @Transactional
    public String drawCard(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Partida no encontrada"));

        String message = performDraw(game);
        scheduleAiTurnsIfNeeded(game);
        return message;
    }

    @Transactional
    public String stand(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Partida no encontrada"));

        String message = performStand(game);
        scheduleAiTurnsIfNeeded(game);
        return message;
    }

    private String performDraw(Game game) {
        game.setLastDuplicateCard(null);
        game.setLastDuplicatePlayerId(null);
        Player currentPlayer = game.getPlayers().get(game.getCurrentPlayerTurnIndex());

        RoundPlayer roundPlayer = roundPlayerRepository
                .findByPlayerIdAndGameIdAndRoundNumber(currentPlayer.getId(), game.getId(), game.getCurrentRound())
                .orElseGet(() -> createRoundPlayer(currentPlayer, game));

        if (roundPlayer.getStatus() != RoundPlayerStatus.ACTIVE) {
            throw new RuntimeException("Este jugador ya no puede pedir cartas");
        }

        int card = deckService.drawCard(game);

        if (isNumberCard(card)) {
            return handleNumberCard(game, currentPlayer, roundPlayer, card);
        }
        if (card == FREEZE) {
            return handleFreeze(game, currentPlayer, roundPlayer);
        }
        if (card == FLIP_THREE) {
            return handleFlipThree(game, currentPlayer, roundPlayer);
        }
        if (card == SECOND_CHANCE) {
            return handleSecondChance(game, currentPlayer, roundPlayer);
        }
        if (card >= 200) {
            return handleModifierCard(game, currentPlayer, roundPlayer, card);
        }

        return "Carta desconocida.";
    }

    private String handleNumberCard(Game game, Player currentPlayer, RoundPlayer roundPlayer, int card) {
        if (roundPlayer.getCurrentCards().contains(card)) {
            if (roundPlayer.isHasSecondChance()) {
                roundPlayer.setHasSecondChance(false);
                roundPlayerRepository.save(roundPlayer);
                advanceTurn(game);
                String message = "Segunda Oportunidad! " + currentPlayer.getName() + " se salvó del duplicado de " + card;
                game.setLastMessage(message);
                gameRepository.save(game);
                return message;
            }

            roundPlayer.getCurrentCards().add(card);
            roundPlayer.setStatus(RoundPlayerStatus.ELIMINATED);
            roundPlayer.setRoundPoints(0);
            roundPlayerRepository.save(roundPlayer);
            
            game.setLastDuplicateCard(card);
            game.setLastDuplicatePlayerId(currentPlayer.getId());
            
            advanceTurn(game);
            checkEndOfRound(game);
            String message = "Carta repetida. " + currentPlayer.getName() + " ha sido eliminado.";
            game.setLastMessage(message);
            gameRepository.save(game);
            return message;
        }

        roundPlayer.getCurrentCards().add(card);
        roundPlayer.setRoundPoints(calculateRawSum(roundPlayer));
        roundPlayerRepository.save(roundPlayer);
        game.setLastMessage(currentPlayer.getName() + " sacó un " + card);
        gameRepository.save(game);

        if (getNumberCardCount(roundPlayer) == FLIP7_COUNT) {
            int finalPoints = calculateRoundScore(roundPlayer);
            roundPlayer.setRoundPoints(finalPoints);
            roundPlayer.setStatus(RoundPlayerStatus.STANDING);
            roundPlayerRepository.save(roundPlayer);
            checkEndOfRound(game);
            String message = "FLIP 7! " + currentPlayer.getName() + " completó 7 cartas.";
            game.setLastMessage(message);
            gameRepository.save(game);
            return message;
        }

        advanceTurn(game);
        return currentPlayer.getName() + " sacó un " + card;
    }

    private String handleFreeze(Game game, Player currentPlayer, RoundPlayer roundPlayer) {
        int score = calculateRoundScore(roundPlayer);
        roundPlayer.setRoundPoints(score);
        roundPlayer.setStatus(RoundPlayerStatus.STANDING);
        roundPlayerRepository.save(roundPlayer);
        advanceTurn(game);
        checkEndOfRound(game);
        String message = "FREEZE! " + currentPlayer.getName() + " se congela con " + score + " puntos.";
        game.setLastMessage(message);
        gameRepository.save(game);
        return message;
    }

    private String handleFlipThree(Game game, Player currentPlayer, RoundPlayer roundPlayer) {
        String message = "FLIP THREE! " + currentPlayer.getName() + " obtuvo la carta especial (robos adicionales desactivados).";
        game.setLastMessage(message);
        gameRepository.save(game);

        advanceTurn(game);
        checkEndOfRound(game);

        return message;
    }

    private String handleSecondChance(Game game, Player currentPlayer, RoundPlayer roundPlayer) {
        if (roundPlayer.isHasSecondChance()) {
            advanceTurn(game);
            String message = currentPlayer.getName() + " ya tenía una Segunda Oportunidad. La nueva se descarta.";
            game.setLastMessage(message);
            gameRepository.save(game);
            return message;
        }

        roundPlayer.setHasSecondChance(true);
        roundPlayerRepository.save(roundPlayer);
        advanceTurn(game);
        String message = currentPlayer.getName() + " recibió una Segunda Oportunidad.";
        game.setLastMessage(message);
        gameRepository.save(game);
        return message;
    }

    private String handleModifierCard(Game game, Player currentPlayer, RoundPlayer roundPlayer, int card) {
        applyModifier(roundPlayer, card);
        roundPlayerRepository.save(roundPlayer);

        String label = card == X2 ? "x2" : "+" + ((card - 200) * 2);
        String message = currentPlayer.getName() + " recibió modificador " + label;
        game.setLastMessage(message);
        gameRepository.save(game);

        advanceTurn(game);
        checkEndOfRound(game);

        return message;
    }

    private void applyModifier(RoundPlayer roundPlayer, int card) {
        if (card == X2) {
            roundPlayer.setHasX2Multiplier(true);
        } else if (card >= 201 && card <= 205) {
            roundPlayer.setModifierBonus(roundPlayer.getModifierBonus() + (card - 200) * 2);
        }
        roundPlayer.getModifierCardValues().add(card);
    }

    private boolean isNumberCard(int card) {
        return card >= 0 && card <= 12;
    }

    private int getNumberCardCount(RoundPlayer roundPlayer) {
        return (int) roundPlayer.getCurrentCards().stream()
                .filter(c -> c >= 0 && c <= 12)
                .count();
    }

    private int calculateRawSum(RoundPlayer roundPlayer) {
        return roundPlayer.getCurrentCards().stream()
                .filter(c -> c >= 0 && c <= 12)
                .mapToInt(Integer::intValue)
                .sum();
    }

    private int calculateRoundScore(RoundPlayer roundPlayer) {
        int sum = calculateRawSum(roundPlayer);
        if (roundPlayer.isHasX2Multiplier()) {
            sum *= 2;
        }
        sum += roundPlayer.getModifierBonus();

        int numberCount = getNumberCardCount(roundPlayer);
        if (numberCount >= FLIP7_COUNT) {
            sum += FLIP7_BONUS;
        }

        return sum;
    }

    private String performStand(Game game) {
        game.setLastDuplicateCard(null);
        game.setLastDuplicatePlayerId(null);
        Player currentPlayer = game.getPlayers().get(game.getCurrentPlayerTurnIndex());

        RoundPlayer roundPlayer = roundPlayerRepository
                .findByPlayerIdAndGameIdAndRoundNumber(currentPlayer.getId(), game.getId(), game.getCurrentRound())
                .orElseThrow(() -> new RuntimeException("El jugador no ha pedido ninguna carta aún"));

        if (roundPlayer.getCurrentCards().size() < 1) {
            throw new RuntimeException("Debes pedir al menos una carta antes de plantarte");
        }

        if (roundPlayer.getStatus() == RoundPlayerStatus.ELIMINATED)
            throw new IllegalArgumentException("player was eliminated");

        int score = calculateRoundScore(roundPlayer);
        roundPlayer.setRoundPoints(score);
        roundPlayer.setStatus(RoundPlayerStatus.STANDING);
        roundPlayerRepository.save(roundPlayer);

        advanceTurn(game);
        checkEndOfRound(game);
        String message = currentPlayer.getName() + " se ha plantado con " + score + " puntos.";
        game.setLastMessage(message);
        gameRepository.save(game);
        return message;
    }

    private void scheduleAiTurnsIfNeeded(Game game) {
        if (game.getGameStatus() != GameStatus.PLAYING || !isCurrentPlayerAi(game)) {
            return;
        }

        aiTurnExecutor.schedule(() -> transactionTemplate.executeWithoutResult(status -> {
            Game refreshedGame = gameRepository.findById(game.getId())
                    .orElseThrow(() -> new RuntimeException("Partida no encontrada"));
            resolveAiTurns(refreshedGame);
        }), aiTurnDelayMs, TimeUnit.MILLISECONDS);
    }

    private void resolveAiTurns(Game game) {
        if (game.getGameStatus() != GameStatus.PLAYING || !isCurrentPlayerAi(game)) {
            return;
        }

        Player currentPlayer = game.getPlayers().get(game.getCurrentPlayerTurnIndex());
        RoundPlayer roundPlayer = roundPlayerRepository
                .findByPlayerIdAndGameIdAndRoundNumber(currentPlayer.getId(), game.getId(), game.getCurrentRound())
                .orElseGet(() -> createRoundPlayer(currentPlayer, game));

        if (roundPlayer.getStatus() != RoundPlayerStatus.ACTIVE) {
            advanceTurn(game);
            scheduleAiTurnsIfNeeded(game);
            return;
        }

        OllamaAiService.AiDecision decision = ollamaAiService.decide(game, currentPlayer, roundPlayer, game.getPlayers());
        String action = normalizeAiDecision(decision.decision());
        game.setAiReason(decision.reason());

        if ("stand".equals(action) && roundPlayer.getCurrentCards().isEmpty()) {
            action = "hit";
        }

        String actionResult = "stand".equals(action) ? performStand(game) : performDraw(game);
        game.setLastMessage(actionResult);
        gameRepository.save(game);

        // Si después de la acción sigue siendo el turno de una IA, programamos la siguiente
        if (game.getGameStatus() == GameStatus.PLAYING && isCurrentPlayerAi(game)) {
            scheduleAiTurnsIfNeeded(game);
        }
    }

    private boolean isCurrentPlayerAi(Game game) {
        Player currentPlayer = game.getPlayers().get(game.getCurrentPlayerTurnIndex());
        return currentPlayer.isAiControlled();
    }

    private String normalizeAiDecision(String decision) {
        if (decision == null) {
            return "hit";
        }

        return switch (decision.trim().toLowerCase()) {
            case "stand" -> "stand";
            case "play" -> "hit";
            default -> "hit";
        };
    }

    private void advanceTurn(Game game) {
        // Primero verificamos si la ronda debe terminar antes de mover el turno
        if (isRoundOver(game)) {
            checkEndOfRound(game);
            return;
        }

        List<Player> players = game.getPlayers();
        int total = players.size();
        int next = (game.getCurrentPlayerTurnIndex() + 1) % total;

        List<RoundPlayer> roundPlayers = roundPlayerRepository
                .findByGameIdAndRoundNumber(game.getId(), game.getCurrentRound());

        int attempts = 0;
        while (attempts < total) {
            int finalNext = next;
            boolean isStillPlaying = roundPlayers.stream()
                    .filter(rp -> rp.getPlayer().getId().equals(players.get(finalNext).getId()))
                    .findFirst()
                    .map(rp -> rp.getStatus() == RoundPlayerStatus.ACTIVE)
                    .orElse(true);

            if (isStillPlaying) break;
            next = (next + 1) % total;
            attempts++;
        }

        game.setCurrentPlayerTurnIndex(next);
        gameRepository.save(game);
    }

    private boolean isRoundOver(Game game) {
        List<RoundPlayer> roundPlayers = roundPlayerRepository
                .findByGameIdAndRoundNumber(game.getId(), game.getCurrentRound());

        // Si alguien tiene 7 cartas, la ronda termina inmediatamente
        boolean someoneHasSevenCards = roundPlayers.stream()
                .anyMatch(rp -> getNumberCardCount(rp) >= FLIP7_COUNT);
        if (someoneHasSevenCards) return true;

        // Si todos los jugadores están STANDING o ELIMINATED, la ronda termina
        return game.getPlayers().stream().allMatch(player ->
                roundPlayers.stream()
                        .filter(rp -> rp.getPlayer().getId().equals(player.getId()))
                        .findFirst()
                        .map(rp -> rp.getStatus() != RoundPlayerStatus.ACTIVE)
                        .orElse(false)
        );
    }

    private void checkEndOfRound(Game game) {
        List<RoundPlayer> roundPlayers = roundPlayerRepository
                .findByGameIdAndRoundNumber(game.getId(), game.getCurrentRound());

        if (isRoundOver(game)) {
            finishRound(game, roundPlayers);
        }
    }

    private void finishRound(Game game, List<RoundPlayer> roundPlayers) {
        roundPlayers.stream()
                .filter(rp -> rp.getStatus() != RoundPlayerStatus.ELIMINATED)
                .forEach(rp -> {
                    if (rp.getStatus() != RoundPlayerStatus.STANDING) {
                        rp.setRoundPoints(calculateRoundScore(rp));
                        rp.setStatus(RoundPlayerStatus.STANDING);
                        roundPlayerRepository.save(rp);
                    }

                    Player player = rp.getPlayer();
                    player.setTotalPoints(player.getTotalPoints() + rp.getRoundPoints());
                    playerRepository.save(player);
                });

        Player winner = game.getPlayers().stream()
                .max((left, right) -> Integer.compare(left.getTotalPoints(), right.getTotalPoints()))
                .orElse(null);

        if (winner != null && winner.getTotalPoints() >= POINTS_TO_WIN) {
            game.setGameStatus(GameStatus.FINISHED);
            game.setWinner(winner);
        } else {
            int nextStarting = (game.getStartingPlayerIndex() + 1) % game.getPlayers().size();
            game.setStartingPlayerIndex(nextStarting);
            game.setCurrentPlayerTurnIndex(nextStarting);
            game.setCurrentRound(game.getCurrentRound() + 1);
            
            // Limpiar estados de alerta de la ronda anterior
            game.setLastDuplicateCard(null);
            game.setLastDuplicatePlayerId(null);
            game.setAiReason(null);
        }

        gameRepository.save(game);
        
        // Si el primer jugador de la nueva ronda es IA, programar su turno
        if (game.getGameStatus() == GameStatus.PLAYING && isCurrentPlayerAi(game)) {
            scheduleAiTurnsIfNeeded(game);
        }
    }

    private RoundPlayer createRoundPlayer(Player player, Game game) {
        RoundPlayer rp = new RoundPlayer();
        rp.setPlayer(player);
        rp.setGame(game);
        rp.setRoundNumber(game.getCurrentRound());
        return roundPlayerRepository.save(rp);
    }

    @PreDestroy
    void shutdownExecutor() {
        aiTurnExecutor.shutdownNow();
    }
}
