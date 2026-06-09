package com.flip7.game.unit;

import com.flip7.game.DTO.FullGameStateDTO;
import com.flip7.game.DTO.PlayerDTO;
import com.flip7.game.GameStatus;
import com.flip7.game.RoundPlayerStatus;
import com.flip7.game.model.Deck;
import com.flip7.game.model.Game;
import com.flip7.game.model.Player;
import com.flip7.game.model.RoundPlayer;
import com.flip7.game.repository.DeckRepository;
import com.flip7.game.repository.GameRepository;
import com.flip7.game.repository.PlayerRepository;
import com.flip7.game.repository.RoundPlayerRepository;
import com.flip7.game.service.DeckService;
import com.flip7.game.service.GameService;
import com.flip7.game.service.PlayerService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GameServiceTest {

    @Test
    void normalizePlayerName_returnsDefaultWhenNullOrBlank() throws Exception {
        GameService gameService = new GameService(
                mock(GameRepository.class),
                mock(PlayerService.class),
                mock(PlayerRepository.class),
                mock(DeckService.class),
                mock(DeckRepository.class),
                mock(RoundPlayerRepository.class)
        );

        assertThat(invokeNormalizePlayerName(gameService, null)).isEqualTo("Jugador");
        assertThat(invokeNormalizePlayerName(gameService, "   ")).isEqualTo("Jugador");
    }

    @Test
    void normalizePlayerName_trimsAndKeepsProvidedValue() throws Exception {
        GameService gameService = new GameService(
                mock(GameRepository.class),
                mock(PlayerService.class),
                mock(PlayerRepository.class),
                mock(DeckService.class),
                mock(DeckRepository.class),
                mock(RoundPlayerRepository.class)
        );

        assertThat(invokeNormalizePlayerName(gameService, "  Ana  ")).isEqualTo("Ana");
    }

    @Test
    void getFullState_mapsDuplicateAlertWinnerAndScoreboard() {
        GameRepository gameRepository = mock(GameRepository.class);
        PlayerService playerService = mock(PlayerService.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        DeckService deckService = mock(DeckService.class);
        DeckRepository deckRepository = mock(DeckRepository.class);
        RoundPlayerRepository roundPlayerRepository = mock(RoundPlayerRepository.class);

        GameService gameService = new GameService(
                gameRepository,
                playerService,
                playerRepository,
                deckService,
                deckRepository,
                roundPlayerRepository
        );

        Game game = new Game();
        game.setId(1L);
        game.setGameStatus(GameStatus.PLAYING);
        game.setCurrentRound(1);
        game.setCurrentPlayerTurnIndex(0);
        game.setStartingPlayerIndex(0);
        game.setLastMessage("ok");
        game.setAiReason("reason");
        game.setLastDuplicateCard(7);
        game.setLastDuplicatePlayerId(11L);

        Player p1 = player(11L, "Alice", 30, false, game);
        Player p2 = player(12L, "Bot", 40, true, game);
        game.setPlayers(new ArrayList<>(List.of(p1, p2)));
        game.setWinner(p2);

        RoundPlayer rp1 = roundPlayer(game, p1, List.of(2, 3), RoundPlayerStatus.ACTIVE, 5);
        rp1.setHasSecondChance(true);
        rp1.setModifierCardValues(List.of(201));

        RoundPlayer rp2 = roundPlayer(game, p2, List.of(4), RoundPlayerStatus.STANDING, 4);

        Deck deck = new Deck();
        deck.reset();

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(playerRepository.findByGameId(1L)).thenReturn(List.of(p1, p2));
        when(playerRepository.findById(11L)).thenReturn(Optional.of(p1));
        when(roundPlayerRepository.findByGameIdAndRoundNumber(1L, 1)).thenReturn(List.of(rp1, rp2));
        when(deckRepository.findByGameId(1L)).thenReturn(Optional.of(deck));

        FullGameStateDTO dto = gameService.getFullState(1L);

        assertThat(dto.getGameId()).isEqualTo(1L);
        assertThat(dto.getStatus()).isEqualTo("PLAYING");
        assertThat(dto.getCurrentPlayerTurnId()).isEqualTo(11L);
        assertThat(dto.getDuplicateAlert()).isNotNull();
        assertThat(dto.getDuplicateAlert().getPlayerName()).isEqualTo("Alice");
        assertThat(dto.getDuplicateAlert().getCardValue()).isEqualTo(7);
        assertThat(dto.getWinner()).isNotNull();
        assertThat(dto.getWinner().getName()).isEqualTo("Bot");
        assertThat(dto.getScoreboard()).hasSize(2);
        assertThat(dto.getScoreboard().get(0).getName()).isEqualTo("Bot");
        assertThat(dto.getPlayers().get(0).isHasSecondChance()).isTrue();
        assertThat(dto.getPlayers().get(0).getModifierCardValues()).containsExactly(201);
    }

    @Test
    void getFullState_skipsDuplicateAlertWhenPlayerWasNotFound() {
        GameRepository gameRepository = mock(GameRepository.class);
        PlayerService playerService = mock(PlayerService.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        DeckService deckService = mock(DeckService.class);
        DeckRepository deckRepository = mock(DeckRepository.class);
        RoundPlayerRepository roundPlayerRepository = mock(RoundPlayerRepository.class);

        GameService gameService = new GameService(
                gameRepository,
                playerService,
                playerRepository,
                deckService,
                deckRepository,
                roundPlayerRepository
        );

        Game game = new Game();
        game.setId(5L);
        game.setGameStatus(GameStatus.PLAYING);
        game.setCurrentRound(1);
        game.setCurrentPlayerTurnIndex(0);
        game.setStartingPlayerIndex(0);
        game.setLastDuplicateCard(9);
        game.setLastDuplicatePlayerId(111L);

        Player p1 = player(1L, "A", 10, false, game);
        game.setPlayers(List.of(p1));

        when(gameRepository.findById(5L)).thenReturn(Optional.of(game));
        when(playerRepository.findByGameId(5L)).thenReturn(List.of(p1));
        when(playerRepository.findById(111L)).thenReturn(Optional.empty());
        when(roundPlayerRepository.findByGameIdAndRoundNumber(5L, 1)).thenReturn(List.of());
        when(deckRepository.findByGameId(5L)).thenReturn(Optional.empty());

        FullGameStateDTO dto = gameService.getFullState(5L);

        assertThat(dto.getDuplicateAlert()).isNull();
    }

    @Test
    void getFullState_usesDefaultRoundStateWhenNoRoundPlayerExists() {
        GameRepository gameRepository = mock(GameRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        RoundPlayerRepository roundPlayerRepository = mock(RoundPlayerRepository.class);
        DeckRepository deckRepository = mock(DeckRepository.class);

        GameService gameService = new GameService(
                gameRepository,
                mock(PlayerService.class),
                playerRepository,
                mock(DeckService.class),
                deckRepository,
                roundPlayerRepository
        );

        Game game = new Game();
        game.setId(9L);
        game.setGameStatus(GameStatus.PLAYING);
        game.setCurrentRound(2);
        game.setCurrentPlayerTurnIndex(0);
        game.setStartingPlayerIndex(0);

        Player player = player(21L, "Only", 10, false, game);
        game.setPlayers(List.of(player));

        when(gameRepository.findById(9L)).thenReturn(Optional.of(game));
        when(playerRepository.findByGameId(9L)).thenReturn(List.of(player));
        when(roundPlayerRepository.findByGameIdAndRoundNumber(9L, 2)).thenReturn(List.of());
        when(deckRepository.findByGameId(9L)).thenReturn(Optional.empty());

        FullGameStateDTO dto = gameService.getFullState(9L);

        assertThat(dto.getPlayers()).hasSize(1);
        assertThat(dto.getPlayers().get(0).getStatus()).isEqualTo("ACTIVE");
        assertThat(dto.getPlayers().get(0).getRoundCards()).isEmpty();
        assertThat(dto.getPlayers().get(0).getRoundPoints()).isZero();
        assertThat(dto.getDeckRemaining()).isZero();
    }

    @Test
    void getScoreboard_sortsDescendingByPoints() {
        GameRepository gameRepository = mock(GameRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);

        GameService gameService = new GameService(
                gameRepository,
                mock(PlayerService.class),
                playerRepository,
                mock(DeckService.class),
                mock(DeckRepository.class),
                mock(RoundPlayerRepository.class)
        );

        Game game = new Game();
        game.setId(1L);

        Player a = player(1L, "A", 15, false, game);
        Player b = player(2L, "B", 20, false, game);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(playerRepository.findByGameId(1L)).thenReturn(List.of(a, b));

        List<PlayerDTO> scoreboard = gameService.getScoreboard(1L);

        assertThat(scoreboard).hasSize(2);
        assertThat(scoreboard.get(0).getName()).isEqualTo("B");
    }

    @Test
    void getWinner_returnsNullWhenNoWinnerStored() {
        GameRepository gameRepository = mock(GameRepository.class);
        GameService gameService = new GameService(
                gameRepository,
                mock(PlayerService.class),
                mock(PlayerRepository.class),
                mock(DeckService.class),
                mock(DeckRepository.class),
                mock(RoundPlayerRepository.class)
        );

        Game game = new Game();
        game.setId(4L);
        when(gameRepository.findById(4L)).thenReturn(Optional.of(game));

        assertThat(gameService.getWinner(4L)).isNull();
    }

    @Test
    void findGameById_throwsWhenNotFound() {
        GameRepository gameRepository = mock(GameRepository.class);
        GameService gameService = new GameService(
                gameRepository,
                mock(PlayerService.class),
                mock(PlayerRepository.class),
                mock(DeckService.class),
                mock(DeckRepository.class),
                mock(RoundPlayerRepository.class)
        );

        when(gameRepository.findById(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.findGameById(123L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Partida no encontrada");
    }

    @Test
    void advanceTurn_and_startNextRound_updateIndexes() {
        GameRepository gameRepository = mock(GameRepository.class);
        GameService gameService = new GameService(
                gameRepository,
                mock(PlayerService.class),
                mock(PlayerRepository.class),
                mock(DeckService.class),
                mock(DeckRepository.class),
                mock(RoundPlayerRepository.class)
        );

        Game game = new Game();
        game.setCurrentPlayerTurnIndex(0);
        game.setStartingPlayerIndex(0);
        game.setCurrentRound(1);
        game.setPlayers(List.of(new Player(), new Player(), new Player()));

        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        gameService.advanceTurn(game);
        assertThat(game.getCurrentPlayerTurnIndex()).isEqualTo(1);

        gameService.startNextRound(game);
        assertThat(game.getStartingPlayerIndex()).isEqualTo(1);
        assertThat(game.getCurrentPlayerTurnIndex()).isEqualTo(1);
        assertThat(game.getCurrentRound()).isEqualTo(2);
    }

    private Player player(Long id, String name, int points, boolean ai, Game game) {
        Player player = new Player();
        player.setId(id);
        player.setName(name);
        player.setTotalPoints(points);
        player.setAiControlled(ai);
        player.setGame(game);
        return player;
    }

    private RoundPlayer roundPlayer(Game game, Player player, List<Integer> cards, RoundPlayerStatus status, int points) {
        RoundPlayer roundPlayer = new RoundPlayer();
        roundPlayer.setGame(game);
        roundPlayer.setPlayer(player);
        roundPlayer.setRoundNumber(game.getCurrentRound());
        roundPlayer.setCurrentCards(cards);
        roundPlayer.setStatus(status);
        roundPlayer.setRoundPoints(points);
        return roundPlayer;
    }

    private String invokeNormalizePlayerName(GameService gameService, String value) throws Exception {
        Method method = GameService.class.getDeclaredMethod("normalizePlayerName", String.class);
        method.setAccessible(true);
        return (String) method.invoke(gameService, value);
    }
}