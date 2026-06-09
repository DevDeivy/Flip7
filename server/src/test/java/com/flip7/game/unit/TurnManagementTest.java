package com.flip7.game.unit;

import com.flip7.game.RoundPlayerStatus;
import com.flip7.game.model.Game;
import com.flip7.game.model.Player;
import com.flip7.game.model.RoundPlayer;
import com.flip7.game.GameStatus;
import com.flip7.game.repository.GameRepository;
import com.flip7.game.repository.PlayerRepository;
import com.flip7.game.repository.RoundPlayerRepository;
import com.flip7.game.service.DeckService;
import com.flip7.game.service.OllamaAiService;
import com.flip7.game.service.TurnService;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

class TurnManagementTest {

    @Test
    void drawCard_skipsStandingAndEliminatedPlayers() {
        RoundPlayerRepository roundPlayerRepository = mock(RoundPlayerRepository.class);
        GameRepository gameRepository = mock(GameRepository.class);
        DeckService deckService = mock(DeckService.class);

        TurnService turnService = new TurnService(
                roundPlayerRepository,
                mock(PlayerRepository.class),
                gameRepository,
                deckService,
                mock(OllamaAiService.class),
                mock(TransactionTemplate.class)
        );

        Game game = createGame();
        RoundPlayer current = roundPlayer(game, game.getPlayers().get(0), RoundPlayerStatus.ACTIVE, List.of());
        RoundPlayer standing = roundPlayer(game, game.getPlayers().get(1), RoundPlayerStatus.STANDING, List.of(3));
        RoundPlayer eliminated = roundPlayer(game, game.getPlayers().get(2), RoundPlayerStatus.ELIMINATED, List.of(4, 4));
        RoundPlayer nextActive = roundPlayer(game, game.getPlayers().get(3), RoundPlayerStatus.ACTIVE, List.of(2));

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roundPlayerRepository.save(any(RoundPlayer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roundPlayerRepository.findByPlayerIdAndGameIdAndRoundNumber(eq(1L), eq(1L), eq(1))).thenReturn(Optional.of(current));
        when(roundPlayerRepository.findByGameIdAndRoundNumber(eq(1L), eq(1))).thenReturn(List.of(current, standing, eliminated, nextActive));
        when(deckService.drawCard(game)).thenReturn(6);

        turnService.drawCard(1L);

        assertThat(game.getCurrentPlayerTurnIndex()).isEqualTo(3);
    }

        @Test
        void normalizeAiDecision_mapsExpectedValues() throws Exception {
        TurnService turnService = new TurnService(
            mock(RoundPlayerRepository.class),
            mock(PlayerRepository.class),
            mock(GameRepository.class),
            mock(DeckService.class),
            mock(OllamaAiService.class),
            mock(TransactionTemplate.class)
        );

        assertThat((String) invoke(turnService, "normalizeAiDecision", new Class[]{String.class}, (Object) null)).isEqualTo("hit");
        assertThat((String) invoke(turnService, "normalizeAiDecision", new Class[]{String.class}, "stand")).isEqualTo("stand");
        assertThat((String) invoke(turnService, "normalizeAiDecision", new Class[]{String.class}, "play")).isEqualTo("hit");
        assertThat((String) invoke(turnService, "normalizeAiDecision", new Class[]{String.class}, "HIT")).isEqualTo("hit");
        assertThat((String) invoke(turnService, "normalizeAiDecision", new Class[]{String.class}, "weird")).isEqualTo("hit");
        }

        @Test
        void scheduleAiTurnsIfNeeded_branchesForPlayingAndAi() throws Exception {
        RoundPlayerRepository roundPlayerRepository = mock(RoundPlayerRepository.class);
        GameRepository gameRepository = mock(GameRepository.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);

        TurnService turnService = new TurnService(
            roundPlayerRepository,
            mock(PlayerRepository.class),
            gameRepository,
            mock(DeckService.class),
            mock(OllamaAiService.class),
            transactionTemplate
        );

        setField(turnService, "aiTurnDelayMs", Long.MAX_VALUE);
        doAnswer(invocation -> null).when(transactionTemplate).executeWithoutResult(any());

        Game notPlaying = createAiGame();
        notPlaying.setGameStatus(GameStatus.FINISHED);
        invoke(turnService, "scheduleAiTurnsIfNeeded", new Class[]{Game.class}, notPlaying);

        Game humanTurn = createGame();
        humanTurn.setGameStatus(GameStatus.PLAYING);
        invoke(turnService, "scheduleAiTurnsIfNeeded", new Class[]{Game.class}, humanTurn);

        Game aiTurn = createAiGame();
        aiTurn.setGameStatus(GameStatus.PLAYING);
        invoke(turnService, "scheduleAiTurnsIfNeeded", new Class[]{Game.class}, aiTurn);
        }

        @Test
        void resolveAiTurns_convertsStandWithoutCardsToHit() throws Exception {
        RoundPlayerRepository roundPlayerRepository = mock(RoundPlayerRepository.class);
        GameRepository gameRepository = mock(GameRepository.class);
        DeckService deckService = mock(DeckService.class);
        OllamaAiService aiService = mock(OllamaAiService.class);

        TurnService turnService = new TurnService(
            roundPlayerRepository,
            mock(PlayerRepository.class),
            gameRepository,
            deckService,
            aiService,
            mock(TransactionTemplate.class)
        );

        Game game = createAiGame();
        game.setGameStatus(GameStatus.PLAYING);
        RoundPlayer aiRound = roundPlayer(game, game.getPlayers().get(0), RoundPlayerStatus.ACTIVE, List.of());

        when(roundPlayerRepository.findByPlayerIdAndGameIdAndRoundNumber(eq(1L), eq(1L), eq(1)))
            .thenReturn(Optional.of(aiRound));
        when(roundPlayerRepository.findByGameIdAndRoundNumber(eq(1L), eq(1)))
            .thenReturn(List.of(aiRound));
        when(roundPlayerRepository.save(any(RoundPlayer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiService.decide(eq(game), any(Player.class), eq(aiRound), any(List.class)))
            .thenReturn(new OllamaAiService.AiDecision("stand", "ia reason"));
        when(deckService.drawCard(game)).thenReturn(5);

        invoke(turnService, "resolveAiTurns", new Class[]{Game.class}, game);

        assertThat(aiRound.getCurrentCards()).contains(5);
        assertThat(game.getAiReason()).isEqualTo("ia reason");
        assertThat(game.getCurrentPlayerTurnIndex()).isEqualTo(1);
        }

        @Test
        void resolveAiTurns_whenCurrentAiRoundPlayerIsNotActive_advancesTurn() throws Exception {
        RoundPlayerRepository roundPlayerRepository = mock(RoundPlayerRepository.class);
        GameRepository gameRepository = mock(GameRepository.class);

        TurnService turnService = new TurnService(
            roundPlayerRepository,
            mock(PlayerRepository.class),
            gameRepository,
            mock(DeckService.class),
            mock(OllamaAiService.class),
            mock(TransactionTemplate.class)
        );

        Game game = createAiGame();
        game.setGameStatus(GameStatus.PLAYING);
        RoundPlayer aiRound = roundPlayer(game, game.getPlayers().get(0), RoundPlayerStatus.STANDING, List.of(7));
        RoundPlayer next = roundPlayer(game, game.getPlayers().get(1), RoundPlayerStatus.ACTIVE, List.of());

        when(roundPlayerRepository.findByPlayerIdAndGameIdAndRoundNumber(eq(1L), eq(1L), eq(1)))
            .thenReturn(Optional.of(aiRound));
        when(roundPlayerRepository.findByGameIdAndRoundNumber(eq(1L), eq(1)))
            .thenReturn(List.of(aiRound, next));
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        invoke(turnService, "resolveAiTurns", new Class[]{Game.class}, game);

        assertThat(game.getCurrentPlayerTurnIndex()).isEqualTo(1);
        }

        @Test
        void resolveAiTurns_returnsImmediatelyWhenGameIsNotPlaying() throws Exception {
        TurnService turnService = new TurnService(
            mock(RoundPlayerRepository.class),
            mock(PlayerRepository.class),
            mock(GameRepository.class),
            mock(DeckService.class),
            mock(OllamaAiService.class),
            mock(TransactionTemplate.class)
        );

        Game game = createAiGame();
        game.setGameStatus(GameStatus.FINISHED);
        game.setCurrentPlayerTurnIndex(0);

        invoke(turnService, "resolveAiTurns", new Class[]{Game.class}, game);

        assertThat(game.getCurrentPlayerTurnIndex()).isEqualTo(0);
        }

        @Test
        void resolveAiTurns_usesStandWhenAiHasCards() throws Exception {
        RoundPlayerRepository roundPlayerRepository = mock(RoundPlayerRepository.class);
        GameRepository gameRepository = mock(GameRepository.class);
        OllamaAiService aiService = mock(OllamaAiService.class);

        TurnService turnService = new TurnService(
            roundPlayerRepository,
            mock(PlayerRepository.class),
            gameRepository,
            mock(DeckService.class),
            aiService,
            mock(TransactionTemplate.class)
        );

        Game game = createAiGame();
        game.setGameStatus(GameStatus.PLAYING);
        RoundPlayer aiRound = roundPlayer(game, game.getPlayers().get(0), RoundPlayerStatus.ACTIVE, List.of(5));

        when(roundPlayerRepository.findByPlayerIdAndGameIdAndRoundNumber(eq(1L), eq(1L), eq(1)))
            .thenReturn(Optional.of(aiRound));
        when(roundPlayerRepository.findByGameIdAndRoundNumber(eq(1L), eq(1)))
            .thenReturn(List.of(aiRound));
        when(roundPlayerRepository.save(any(RoundPlayer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiService.decide(eq(game), any(Player.class), eq(aiRound), any(List.class)))
            .thenReturn(new OllamaAiService.AiDecision("stand", "plantarse"));

        invoke(turnService, "resolveAiTurns", new Class[]{Game.class}, game);

        assertThat(aiRound.getStatus()).isEqualTo(RoundPlayerStatus.STANDING);
        assertThat(game.getAiReason()).isEqualTo("plantarse");
        }

    private Game createGame() {
        Game game = new Game();
        game.setId(1L);
        game.setCurrentRound(1);
        game.setCurrentPlayerTurnIndex(0);

        List<Player> players = new ArrayList<>();
        players.add(player(1L, "Alice", game));
        players.add(player(2L, "Bob", game));
        players.add(player(3L, "Charlie", game));
        players.add(player(4L, "Diana", game));
        game.setPlayers(players);
        return game;
    }

    private Game createAiGame() {
        Game game = createGame();
        game.getPlayers().get(0).setAiControlled(true);
        return game;
    }

    private Player player(Long id, String name, Game game) {
        Player player = new Player();
        player.setId(id);
        player.setName(name);
        player.setGame(game);
        return player;
    }

    private RoundPlayer roundPlayer(Game game, Player player, RoundPlayerStatus status, List<Integer> cards) {
        RoundPlayer rp = new RoundPlayer();
        rp.setGame(game);
        rp.setPlayer(player);
        rp.setRoundNumber(game.getCurrentRound());
        rp.setStatus(status);
        rp.setCurrentCards(new ArrayList<>(cards));
        return rp;
    }

    private Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
