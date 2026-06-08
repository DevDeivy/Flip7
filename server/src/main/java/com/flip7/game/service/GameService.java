package com.flip7.game.service;

import com.flip7.game.DTO.CreateGameDTO;
import com.flip7.game.DTO.FullGameStateDTO;
import com.flip7.game.DTO.FullPlayerStateDTO;
import com.flip7.game.DTO.PlayerDTO;
import com.flip7.game.GameStatus;
import com.flip7.game.model.Deck;
import com.flip7.game.model.Game;
import com.flip7.game.model.Player;
import com.flip7.game.model.RoundPlayer;
import com.flip7.game.repository.DeckRepository;
import com.flip7.game.repository.GameRepository;
import com.flip7.game.repository.PlayerRepository;
import com.flip7.game.repository.RoundPlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GameService {
    private static final int POINTS_TO_WIN = 200;
    private static final String AI_PLAYER_NAME = "FLIP7 AI";

    private final GameRepository gameRepository;
    private final PlayerService playerService;
    private final PlayerRepository playerRepository;
    private final DeckService deckService;
    private final DeckRepository deckRepository;
    private final RoundPlayerRepository roundPlayerRepository;

    @Transactional
    public FullGameStateDTO createGame(CreateGameDTO request) {
        return createGame(request.getPlayers());
    }

    @Transactional
    public FullGameStateDTO createGame(List<String> playersRequest) {
        if (playersRequest.size() < 4 || playersRequest.size() > 8) {
            throw new IllegalArgumentException("Debe haber entre 4 y 8 jugadores");
        }

        return createGameInternal(playersRequest, false);
    }

    @Transactional
    public FullGameStateDTO createAiGame(String playerName) {
        String normalizedPlayerName = normalizePlayerName(playerName);
        return createGameInternal(List.of(normalizedPlayerName, AI_PLAYER_NAME), true);
    }

    private FullGameStateDTO createGameInternal(List<String> playersRequest, boolean markLastPlayerAsAi) {
        Game game = new Game();
        game.setCreatedAt(Instant.now());
        game.setGameStatus(GameStatus.WAITING);
        gameRepository.save(game);

        List<Player> createdPlayers = playerService.createPlayers(playersRequest, game, markLastPlayerAsAi);
        game.setPlayers(createdPlayers);

        game.setGameStatus(GameStatus.PLAYING);
        gameRepository.save(game);

        deckService.createDeck(game);

        return getFullState(game.getId());
    }

    private String normalizePlayerName(String playerName) {
        String normalized = playerName == null ? "" : playerName.trim();
        if (normalized.isEmpty()) {
            return "Jugador";
        }

        return normalized;
    }

    public Game findGameById(Long gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Partida no encontrada"));
    }

    public FullGameStateDTO getFullState(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Partida no encontrada"));

        List<Player> players = playerRepository.findByGameId(gameId);

        List<RoundPlayer> roundPlayers = roundPlayerRepository
                .findByGameIdAndRoundNumber(game.getId(), game.getCurrentRound());

        final List<RoundPlayer> finalRoundPlayers = roundPlayers;

        Deck deck = deckRepository.findByGameId(gameId).orElse(null);

        FullGameStateDTO dto = new FullGameStateDTO();
        dto.setGameId(game.getId());
        dto.setStatus(game.getGameStatus().name());
        dto.setCurrentRound(game.getCurrentRound());
        dto.setCurrentPlayerTurnIndex(game.getCurrentPlayerTurnIndex());
        dto.setCurrentPlayerTurnId(players.isEmpty()
            ? null
            : players.get(game.getCurrentPlayerTurnIndex()).getId());
        dto.setStartingPlayerIndex(game.getStartingPlayerIndex());
        dto.setDeckRemaining(deck != null ? deck.getAvailableCards().size() : 0);
        dto.setLastMessage(game.getLastMessage());
        dto.setAiReason(game.getAiReason());

        if (game.getLastDuplicateCard() != null && game.getLastDuplicatePlayerId() != null) {
            Player p = playerRepository.findById(game.getLastDuplicatePlayerId()).orElse(null);
            if (p != null) {
                com.flip7.game.DTO.DuplicateAlertDTO alert = new com.flip7.game.DTO.DuplicateAlertDTO();
                alert.setPlayerId(String.valueOf(p.getId()));
                alert.setPlayerName(p.getName());
                alert.setCardValue(game.getLastDuplicateCard());
                alert.setMessage("¡" + p.getName() + " sacó un " + game.getLastDuplicateCard() + " repetido y ha sido eliminado!");
                dto.setDuplicateAlert(alert);
            }
        }

        List<FullPlayerStateDTO> playerDTOs = players.stream()
                .map(player -> {
                    FullPlayerStateDTO p = new FullPlayerStateDTO();
                    p.setPlayerId(player.getId());
                    p.setName(player.getName());
                    p.setTotalPoints(player.getTotalPoints());
                    p.setAiControlled(player.isAiControlled());

                    RoundPlayer rp = finalRoundPlayers.stream()
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
        dto.setScoreboard(players.stream()
                .map(player -> {
                    PlayerDTO p = new PlayerDTO();
                    p.setId(player.getId());
                    p.setName(player.getName());
                    p.setTotalPoints(player.getTotalPoints());
                    p.setAiControlled(player.isAiControlled());
                    return p;
                })
                .sorted(Comparator.comparingInt(PlayerDTO::getTotalPoints).reversed())
                .toList());

        if (game.getWinner() != null) {
            Player winner = game.getWinner();
            PlayerDTO winnerDto = new PlayerDTO();
            winnerDto.setId(winner.getId());
            winnerDto.setName(winner.getName());
            winnerDto.setTotalPoints(winner.getTotalPoints());
            winnerDto.setAiControlled(winner.isAiControlled());
            dto.setWinner(winnerDto);
        }

        return dto;
    }

    public List<PlayerDTO> getScoreboard(Long gameId) {
        gameRepository.findById(gameId)
            .orElseThrow(() -> new RuntimeException("Partida no encontrada"));

        return playerRepository.findByGameId(gameId).stream()
                .map(player -> {
                    PlayerDTO dto = new PlayerDTO();
                    dto.setId(player.getId());
                    dto.setName(player.getName());
                    dto.setTotalPoints(player.getTotalPoints());
                    return dto;
                })
                .sorted(Comparator.comparingInt(PlayerDTO::getTotalPoints).reversed())
                .toList();
    }

    public PlayerDTO getWinner(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Partida no encontrada"));

        if (game.getWinner() == null) {
            return null;
        }

        Player winner = game.getWinner();
        PlayerDTO dto = new PlayerDTO();
        dto.setId(winner.getId());
        dto.setName(winner.getName());
        dto.setTotalPoints(winner.getTotalPoints());
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
