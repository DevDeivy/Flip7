package com.flip7.game.service;

import com.flip7.game.DTO.DrawResultDTO;
import com.flip7.game.DTO.StandResultDTO;
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

import java.util.List;

@Service
@RequiredArgsConstructor
public class TurnService {

    private static final int POINTS_TO_WIN = 200;
    private static final int FLIP7_BONUS = 15;
    private static final int FLIP7_COUNT = 7;

    private final RoundPlayerRepository roundPlayerRepository;
    private final PlayerRepository playerRepository;
    private final GameRepository gameRepository;
    private final DeckService deckService;

    public DrawResultDTO drawCard(Long gameId) {
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

        DrawResultDTO result = new DrawResultDTO();
        result.setPlayerName(currentPlayer.getName());
        result.setCardDrawn(card);

        // Carta repetida -> eliminado
        if (roundPlayer.getCurrentCards().contains(card)) {
            roundPlayer.setStatus(RoundPlayerStatus.ELIMINATED);
            roundPlayer.setRoundPoints(0);
            roundPlayerRepository.save(roundPlayer);
            result.setCurrentCards(roundPlayer.getCurrentCards());
            result.setRoundPoints(0);
            result.setStatus(RoundPlayerStatus.ELIMINATED);
            result.setMessage("Carta repetida. " + currentPlayer.getName() + " ha sido eliminado.");
            advanceTurn(game);
            return result;
        }

        // Agregar carta
        roundPlayer.getCurrentCards().add(card);
        roundPlayer.setRoundPoints(roundPlayer.getRoundPoints() + card);

        // Verificar Flip7
        if (roundPlayer.getCurrentCards().size() == FLIP7_COUNT) {
            roundPlayer.setRoundPoints(roundPlayer.getRoundPoints() + FLIP7_BONUS);
            roundPlayer.setStatus(RoundPlayerStatus.STANDING);
            roundPlayerRepository.save(roundPlayer);
            result.setMessage("FLIP 7! " + currentPlayer.getName() + " completó 7 cartas.");
            result.setStatus(RoundPlayerStatus.STANDING);
            advanceTurn(game);
            checkEndOfRound(game);
            return result;
        }

        roundPlayerRepository.save(roundPlayer);
        result.setCurrentCards(roundPlayer.getCurrentCards());
        result.setRoundPoints(roundPlayer.getRoundPoints());
        result.setStatus(RoundPlayerStatus.ACTIVE);
        result.setMessage(currentPlayer.getName() + " sacó un " + card);
        return result;
    }

    public StandResultDTO stand(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Partida no encontrada"));

        Player currentPlayer = game.getPlayers().get(game.getCurrentPlayerTurnIndex());

        RoundPlayer roundPlayer = roundPlayerRepository
                .findByPlayerIdAndGameIdAndRoundNumber(currentPlayer.getId(), gameId, game.getCurrentRound())
                .orElseThrow(() -> new RuntimeException("El jugador no ha pedido ninguna carta aún"));

        if (roundPlayer.getCurrentCards().size() < 1) {
            throw new RuntimeException("Debes pedir al menos una carta antes de plantarte");
        }

        if (roundPlayer.getStatus() == RoundPlayerStatus.ELIMINATED) throw new IllegalArgumentException("player was eliminated");

        roundPlayer.setStatus(RoundPlayerStatus.STANDING);
        roundPlayerRepository.save(roundPlayer);

        StandResultDTO result = new StandResultDTO();
        result.setPlayerName(currentPlayer.getName());
        result.setRoundPoints(roundPlayer.getRoundPoints());
        result.setTotalPoints(currentPlayer.getTotalPoints());
        result.setMessage(currentPlayer.getName() + " se ha plantado con " + roundPlayer.getRoundPoints() + " puntos.");

        advanceTurn(game);
        checkEndOfRound(game);
        return result;
    }

    private void advanceTurn(Game game) {
        List<Player> players = game.getPlayers();
        int total = players.size();
        int next = (game.getCurrentPlayerTurnIndex() + 1) % total;

        // Saltar jugadores que ya no están activos en esta ronda
        List<RoundPlayer> roundPlayers = roundPlayerRepository
                .findByGameIdAndRoundNumber(game.getId(), game.getCurrentRound());

        int attempts = 0;
        while (attempts < total) {
            int finalNext = next;
            boolean isActive = roundPlayers.stream()
                    .filter(rp -> rp.getPlayer().getId().equals(players.get(finalNext).getId()))
                    .findFirst()
                    .map(rp -> rp.getStatus() == RoundPlayerStatus.ACTIVE)
                    .orElse(true); // si no tiene RoundPlayer aún, está activo

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

        boolean allDone = game.getPlayers().stream().allMatch(player ->
                roundPlayers.stream()
                        .filter(rp -> rp.getPlayer().getId().equals(player.getId()))
                        .findFirst()
                        .map(rp -> rp.getStatus() != RoundPlayerStatus.ACTIVE)
                        .orElse(false)
        );

        if (allDone) {
            // Sumar puntos a quienes se plantaron
            roundPlayers.stream()
                    .filter(rp -> rp.getStatus() == RoundPlayerStatus.STANDING)
                    .forEach(rp -> {
                        Player player = rp.getPlayer();
                        player.setTotalPoints(player.getTotalPoints() + rp.getRoundPoints());
                        playerRepository.save(player);
                    });

            // Verificar ganador
            boolean hasWinner = game.getPlayers().stream()
                    .anyMatch(p -> p.getTotalPoints() >= POINTS_TO_WIN);

            if (hasWinner) {
                game.setGameStatus(GameStatus.FINISHED);
            } else {
                // Siguiente ronda
                int nextStarting = (game.getStartingPlayerIndex() + 1) % game.getPlayers().size();
                game.setStartingPlayerIndex(nextStarting);
                game.setCurrentPlayerTurnIndex(nextStarting);
                game.setCurrentRound(game.getCurrentRound() + 1);
            }

            gameRepository.save(game);
        }
    }

    private RoundPlayer createRoundPlayer(Player player, Game game) {
        RoundPlayer rp = new RoundPlayer();
        rp.setPlayer(player);
        rp.setGame(game);
        rp.setRoundNumber(game.getCurrentRound());
        return roundPlayerRepository.save(rp);
    }
}