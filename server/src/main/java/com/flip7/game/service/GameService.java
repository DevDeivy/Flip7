package com.flip7.game.service;

import com.flip7.game.DTO.CreateGameDTO;
import com.flip7.game.DTO.FullGameStateDTO;
import com.flip7.game.DTO.FullPlayerStateDTO;
import com.flip7.game.GameStatus;
import com.flip7.game.model.Deck;
import com.flip7.game.model.Game;
import com.flip7.game.model.Player;
import com.flip7.game.model.RoundPlayer;
import com.flip7.game.repository.DeckRepository;
import com.flip7.game.repository.GameRepository;
import com.flip7.game.repository.RoundPlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GameService {
    private static final int POINTS_TO_WIN = 200;

    private final GameRepository gameRepository;
    private final PlayerService playerService;
    private final DeckService deckService;
    private final DeckRepository deckRepository;
    private final RoundPlayerRepository roundPlayerRepository;

    @Transactional
    public FullGameStateDTO createGame(CreateGameDTO request) {
        if (request.getPlayers().size() < 4 || request.getPlayers().size() > 8) {
            throw new IllegalArgumentException("Debe haber entre 4 y 8 jugadores");
        }

        Game game = new Game();
        game.setGameStatus(GameStatus.WAITING);
        gameRepository.save(game);

        playerService.createPlayers(request.getPlayers(), game);

        game.setGameStatus(GameStatus.PLAYING);
        gameRepository.save(game);

        deckService.createDeck(game);

        return getFullState(game.getId());
    }

    public FullGameStateDTO getFullState(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Partida no encontrada"));

        List<RoundPlayer> roundPlayers = roundPlayerRepository
                .findByGameIdAndRoundNumber(game.getId(), game.getCurrentRound());

        Deck deck = deckRepository.findByGameId(gameId).orElse(null);

        FullGameStateDTO dto = new FullGameStateDTO();
        dto.setGameId(game.getId());
        dto.setStatus(game.getGameStatus().name());
        dto.setCurrentRound(game.getCurrentRound());
        dto.setCurrentPlayerTurnIndex(game.getCurrentPlayerTurnIndex());
        dto.setStartingPlayerIndex(game.getStartingPlayerIndex());
        dto.setDeckRemaining(deck != null ? deck.getAvailableCards().size() : 0);

        List<FullPlayerStateDTO> playerDTOs = game.getPlayers().stream()
                .map(player -> {
                    FullPlayerStateDTO p = new FullPlayerStateDTO();
                    p.setPlayerId(player.getId());
                    p.setName(player.getName());
                    p.setTotalPoints(player.getTotalPoints());

                    RoundPlayer rp = roundPlayers.stream()
                            .filter(r -> r.getPlayer().getId().equals(player.getId()))
                            .findFirst().orElse(null);

                    if (rp != null) {
                        p.setRoundCards(rp.getCurrentCards());
                        p.setStatus(rp.getStatus().name());
                        p.setHasSecondChance(rp.isHasSecondChance());
                        p.setModifierCardValues(rp.getModifierCardValues());
                        p.setRoundPoints(rp.getRoundPoints());
                    } else {
                        p.setRoundCards(List.of());
                        p.setStatus("ACTIVE");
                        p.setHasSecondChance(false);
                        p.setModifierCardValues(List.of());
                        p.setRoundPoints(0);
                    }

                    return p;
                })
                .toList();

        dto.setPlayers(playerDTOs);
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
