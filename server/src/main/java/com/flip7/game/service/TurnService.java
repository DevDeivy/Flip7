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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @Transactional
    public String drawCard(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Partida no encontrada"));

        Player currentPlayer = game.getPlayers().get(game.getCurrentPlayerTurnIndex());

        RoundPlayer roundPlayer = roundPlayerRepository
                .findByPlayerIdAndGameIdAndRoundNumber(currentPlayer.getId(), gameId, game.getCurrentRound())
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
                return "Segunda Oportunidad! " + currentPlayer.getName() + " se salvó del duplicado de " + card;
            }

            roundPlayer.setStatus(RoundPlayerStatus.ELIMINATED);
            roundPlayer.setRoundPoints(0);
            roundPlayerRepository.save(roundPlayer);
            advanceTurn(game);
            checkEndOfRound(game);
            return "Carta repetida. " + currentPlayer.getName() + " ha sido eliminado.";
        }

        roundPlayer.getCurrentCards().add(card);
        roundPlayer.setRoundPoints(calculateRawSum(roundPlayer));
        roundPlayerRepository.save(roundPlayer);

        if (getNumberCardCount(roundPlayer) == FLIP7_COUNT) {
            int finalPoints = calculateRoundScore(roundPlayer);
            roundPlayer.setRoundPoints(finalPoints);
            roundPlayer.setStatus(RoundPlayerStatus.STANDING);
            roundPlayerRepository.save(roundPlayer);
            checkEndOfRound(game);
            return "FLIP 7! " + currentPlayer.getName() + " completó 7 cartas.";
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
        return "FREEZE! " + currentPlayer.getName() + " se congela con " + score + " puntos.";
    }

    private String handleFlipThree(Game game, Player currentPlayer, RoundPlayer roundPlayer) {
        StringBuilder sb = new StringBuilder("FLIP THREE! " + currentPlayer.getName() + " recibe: ");
        boolean stopped = false;

        for (int i = 0; i < 3; i++) {
            if (deckService.isDeckEmpty(game)) break;
            if (stopped) break;

            int card = deckService.drawCard(game);
            sb.append(card).append(" ");

            if (isNumberCard(card)) {
                boolean isDuplicate = roundPlayer.getCurrentCards().contains(card);

                if (isDuplicate) {
                    if (roundPlayer.isHasSecondChance()) {
                        roundPlayer.setHasSecondChance(false);
                        sb.append("(salvado) ");
                    } else {
                        roundPlayer.setStatus(RoundPlayerStatus.ELIMINATED);
                        roundPlayer.setRoundPoints(0);
                        stopped = true;
                        sb.append("(duplicado!) ");
                    }
                } else {
                    roundPlayer.getCurrentCards().add(card);
                    roundPlayer.setRoundPoints(calculateRawSum(roundPlayer));

                    if (getNumberCardCount(roundPlayer) >= FLIP7_COUNT) {
                        int finalPoints = calculateRoundScore(roundPlayer);
                        roundPlayer.setRoundPoints(finalPoints);
                        roundPlayer.setStatus(RoundPlayerStatus.STANDING);
                        stopped = true;
                        sb.append("(FLIP 7!) ");
                    }
                }
            } else if (card == FREEZE) {
                roundPlayer.setStatus(RoundPlayerStatus.STANDING);
                int score = calculateRoundScore(roundPlayer);
                roundPlayer.setRoundPoints(score);
                stopped = true;
                sb.append("(FREEZE!) ");
            } else if (card == SECOND_CHANCE && !roundPlayer.isHasSecondChance()) {
                roundPlayer.setHasSecondChance(true);
                sb.append("(Segunda Oportunidad) ");
            } else if (card == FLIP_THREE) {
                sb.append("(Flip Three anidado ignorado) ");
            } else if (card >= 200) {
                applyModifier(roundPlayer, card);
                sb.append("(modificador) ");
            }
        }

        roundPlayerRepository.save(roundPlayer);
        advanceTurn(game);
        checkEndOfRound(game);
        return sb.toString();
    }

    private String handleSecondChance(Game game, Player currentPlayer, RoundPlayer roundPlayer) {
        if (roundPlayer.isHasSecondChance()) {
            advanceTurn(game);
            return currentPlayer.getName() + " ya tenía una Segunda Oportunidad. La nueva se descarta.";
        }

        roundPlayer.setHasSecondChance(true);
        roundPlayerRepository.save(roundPlayer);
        advanceTurn(game);
        return currentPlayer.getName() + " recibió una Segunda Oportunidad.";
    }

    private String handleModifierCard(Game game, Player currentPlayer, RoundPlayer roundPlayer, int card) {
        applyModifier(roundPlayer, card);
        roundPlayerRepository.save(roundPlayer);

        String label = card == X2 ? "x2" : "+" + ((card - 200) * 2);
        return currentPlayer.getName() + " recibió modificador " + label;
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

    @Transactional
    public String stand(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Partida no encontrada"));

        Player currentPlayer = game.getPlayers().get(game.getCurrentPlayerTurnIndex());

        RoundPlayer roundPlayer = roundPlayerRepository
                .findByPlayerIdAndGameIdAndRoundNumber(currentPlayer.getId(), gameId, game.getCurrentRound())
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
        return currentPlayer.getName() + " se ha plantado con " + score + " puntos.";
    }

    private void advanceTurn(Game game) {
        List<Player> players = game.getPlayers();
        int total = players.size();
        int next = (game.getCurrentPlayerTurnIndex() + 1) % total;

        List<RoundPlayer> roundPlayers = roundPlayerRepository
                .findByGameIdAndRoundNumber(game.getId(), game.getCurrentRound());

        int attempts = 0;
        while (attempts < total) {
            int finalNext = next;
            boolean isActive = roundPlayers.stream()
                    .filter(rp -> rp.getPlayer().getId().equals(players.get(finalNext).getId()))
                    .findFirst()
                    .map(rp -> rp.getStatus() == RoundPlayerStatus.ACTIVE)
                    .orElse(true);

            if (isActive) break;
            next = (next + 1) % total;
            attempts++;
        }

        game.setCurrentPlayerTurnIndex(next);
        gameRepository.save(game);
    }

    private void checkEndOfRound(Game game) {
        List<RoundPlayer> roundPlayers = roundPlayerRepository
                .findByGameIdAndRoundNumber(game.getId(), game.getCurrentRound());

        boolean someoneHasSevenCards = roundPlayers.stream()
            .anyMatch(rp -> getNumberCardCount(rp) >= FLIP7_COUNT);

        if (someoneHasSevenCards) {
            finishRound(game, roundPlayers);
            return;
        }

        boolean allDone = game.getPlayers().stream().allMatch(player ->
                roundPlayers.stream()
                        .filter(rp -> rp.getPlayer().getId().equals(player.getId()))
                        .findFirst()
                        .map(rp -> rp.getStatus() != RoundPlayerStatus.ACTIVE)
                        .orElse(false)
        );

        if (allDone) {
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
        }

        gameRepository.save(game);
    }

    private RoundPlayer createRoundPlayer(Player player, Game game) {
        RoundPlayer rp = new RoundPlayer();
        rp.setPlayer(player);
        rp.setGame(game);
        rp.setRoundNumber(game.getCurrentRound());
        return roundPlayerRepository.save(rp);
    }
}
