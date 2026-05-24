package com.flip7.game.service;

import com.flip7.game.DTO.CreateGameDTO;
import com.flip7.game.DTO.GameDTO;
import com.flip7.game.GameStatus;
import com.flip7.game.model.Game;
import com.flip7.game.model.Player;
import com.flip7.game.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class GameService {
    private static final int POINTS_TO_WIN = 200;

    private final GameRepository gameRepository;
    private final PlayerService playerService;
    private final DeckService deckService;

    public GameDTO createGame(CreateGameDTO request) {
        if (request.getPlayers().size() < 4 || request.getPlayers().size() > 8) {
            throw new IllegalArgumentException("Debe haber entre 4 y 8 jugadores");
        }

        Game game = new Game();
        game.setGameStatus(GameStatus.WAITING);
        gameRepository.save(game);

        playerService.createPlayers(request.getPlayers(), game);

        game.setGameStatus(GameStatus.PLAYING);
        gameRepository.save(game);

        GameDTO dto = new GameDTO();
        dto.setId(game.getId());
        dto.setStatus(game.getGameStatus());
        dto.setCurrentRound(game.getCurrentRound());
        dto.setPlayers(playerService.getPlayersByGame(game.getId()));
        deckService.createDeck(game);
        return dto;
    }

    public void advanceTurn(Game game) {
        int total = game.getPlayers().size();
        int next = (game.getCurrentPlayerTurnIndex() + 1) % total;
        game.setCurrentPlayerTurnIndex(next);
        gameRepository.save(game);
    }

    public void startNextRound(Game game) {
        int nextStarting = (game.getStartingPlayerIndex() + 1) % game.getPlayers().size();
        game.setStartingPlayerIndex(nextStarting);
        game.setCurrentPlayerTurnIndex(nextStarting);
        game.setCurrentRound(game.getCurrentRound() + 1);
        gameRepository.save(game);
    }

    public boolean hasWinner(Game game) {
        return game.getPlayers().stream()
                .anyMatch(p -> p.getTotalPoints() >= POINTS_TO_WIN);
    }

    public Player getWinner(Game game) {
        return game.getPlayers().stream()
                .max(Comparator.comparingInt(Player::getTotalPoints))
                .orElseThrow(() -> new RuntimeException("No hay ganador"));
    }
}
