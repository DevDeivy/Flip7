package com.flip7.game.unit;

import com.flip7.game.RoundPlayerStatus;
import com.flip7.game.model.Game;
import com.flip7.game.model.Player;
import com.flip7.game.model.RoundPlayer;
import com.flip7.game.repository.GameRepository;
import com.flip7.game.repository.PlayerRepository;
import com.flip7.game.repository.RoundPlayerRepository;
import com.flip7.game.service.DeckService;
import com.flip7.game.service.OllamaAiService;
import com.flip7.game.service.TurnService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlayerActionTest {

    private GameRepository gameRepository;
    private RoundPlayerRepository roundPlayerRepository;
    private TurnService turnService;

    @BeforeEach
    void setUp() {
        roundPlayerRepository = mock(RoundPlayerRepository.class);
        gameRepository = mock(GameRepository.class);

        turnService = new TurnService(
                roundPlayerRepository,
                mock(PlayerRepository.class),
                gameRepository,
                mock(DeckService.class),
                mock(OllamaAiService.class),
                mock(TransactionTemplate.class)
        );

        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roundPlayerRepository.findByGameIdAndRoundNumber(any(Long.class), any(Integer.class))).thenReturn(List.of());
    }

    @Test
    void stand_failsWhenPlayerDidNotDrawCards() {
        Game game = createGame();
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(roundPlayerRepository.findByPlayerIdAndGameIdAndRoundNumber(eq(1L), eq(1L), eq(1)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> turnService.stand(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no ha pedido ninguna carta");
    }

    @Test
    void stand_failsWhenPlayerWasEliminated() {
        Game game = createGame();
        RoundPlayer roundPlayer = new RoundPlayer();
        roundPlayer.setStatus(RoundPlayerStatus.ELIMINATED);
        roundPlayer.setCurrentCards(List.of(8));

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(roundPlayerRepository.findByPlayerIdAndGameIdAndRoundNumber(eq(1L), eq(1L), eq(1)))
                .thenReturn(Optional.of(roundPlayer));

        assertThatThrownBy(() -> turnService.stand(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eliminated");
    }

            @Test
            void drawCard_duplicateWithoutSecondChance_eliminatesPlayerAndSetsDuplicateAlert() {
            DeckService deckService = mock(DeckService.class);
            turnService = new TurnService(
                roundPlayerRepository,
                mock(PlayerRepository.class),
                gameRepository,
                deckService,
                mock(OllamaAiService.class),
                mock(TransactionTemplate.class)
            );
            when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(roundPlayerRepository.save(any(RoundPlayer.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Game game = createGame();
            RoundPlayer roundPlayer = activeRoundPlayer(game, new ArrayList<>(List.of(8)));

            when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
            when(roundPlayerRepository.findByPlayerIdAndGameIdAndRoundNumber(eq(1L), eq(1L), eq(1)))
                .thenReturn(Optional.of(roundPlayer));
            when(roundPlayerRepository.findByGameIdAndRoundNumber(eq(1L), eq(1)))
                .thenReturn(List.of(roundPlayer));
            when(deckService.drawCard(game)).thenReturn(8);

            String message = turnService.drawCard(1L);

            assertThat(message).contains("eliminado");
            assertThat(roundPlayer.getStatus()).isEqualTo(RoundPlayerStatus.ELIMINATED);
            assertThat(roundPlayer.getRoundPoints()).isZero();
            assertThat(game.getLastDuplicateCard()).isEqualTo(8);
            assertThat(game.getLastDuplicatePlayerId()).isEqualTo(1L);
            }

            @Test
            void drawCard_duplicateWithSecondChance_consumesProtectionAndKeepsPlayerActive() {
            DeckService deckService = mock(DeckService.class);
            turnService = new TurnService(
                roundPlayerRepository,
                mock(PlayerRepository.class),
                gameRepository,
                deckService,
                mock(OllamaAiService.class),
                mock(TransactionTemplate.class)
            );
            when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(roundPlayerRepository.save(any(RoundPlayer.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Game game = createGame();
            RoundPlayer roundPlayer = activeRoundPlayer(game, new ArrayList<>(List.of(6)));
            roundPlayer.setHasSecondChance(true);

            when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
            when(roundPlayerRepository.findByPlayerIdAndGameIdAndRoundNumber(eq(1L), eq(1L), eq(1)))
                .thenReturn(Optional.of(roundPlayer));
            when(roundPlayerRepository.findByGameIdAndRoundNumber(eq(1L), eq(1)))
                .thenReturn(List.of(roundPlayer));
            when(deckService.drawCard(game)).thenReturn(6);

            String message = turnService.drawCard(1L);

            assertThat(message).contains("Segunda Oportunidad");
            assertThat(roundPlayer.isHasSecondChance()).isFalse();
            assertThat(roundPlayer.getStatus()).isEqualTo(RoundPlayerStatus.ACTIVE);
            assertThat(game.getLastDuplicateCard()).isNull();
            assertThat(game.getLastDuplicatePlayerId()).isNull();
            }

            @Test
            void stand_success_appliesX2AndModifierBonus() {
            Game game = createGame();
            RoundPlayer roundPlayer = activeRoundPlayer(game, new ArrayList<>(List.of(2, 3, 4)));
            roundPlayer.setHasX2Multiplier(true);
            roundPlayer.setModifierBonus(6);

            when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
            when(roundPlayerRepository.findByPlayerIdAndGameIdAndRoundNumber(eq(1L), eq(1L), eq(1)))
                .thenReturn(Optional.of(roundPlayer));
            when(roundPlayerRepository.findByGameIdAndRoundNumber(eq(1L), eq(1)))
                .thenReturn(List.of(roundPlayer));
            when(roundPlayerRepository.save(any(RoundPlayer.class))).thenAnswer(invocation -> invocation.getArgument(0));

            String message = turnService.stand(1L);

            assertThat(message).contains("se ha plantado");
            assertThat(roundPlayer.getRoundPoints()).isEqualTo(24);
            assertThat(roundPlayer.getStatus()).isEqualTo(RoundPlayerStatus.STANDING);
            }

            @Test
            void drawCard_flip7_setsStandingAndAddsBonus() {
            DeckService deckService = mock(DeckService.class);
            turnService = new TurnService(
                roundPlayerRepository,
                mock(PlayerRepository.class),
                gameRepository,
                deckService,
                mock(OllamaAiService.class),
                mock(TransactionTemplate.class)
            );
            when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(roundPlayerRepository.save(any(RoundPlayer.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Game game = createGame();
            RoundPlayer roundPlayer = activeRoundPlayer(game, new ArrayList<>(List.of(0, 1, 2, 3, 4, 5)));

            when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
            when(roundPlayerRepository.findByPlayerIdAndGameIdAndRoundNumber(eq(1L), eq(1L), eq(1)))
                .thenReturn(Optional.of(roundPlayer));
            when(roundPlayerRepository.findByGameIdAndRoundNumber(eq(1L), eq(1)))
                .thenReturn(List.of(roundPlayer));
            when(deckService.drawCard(game)).thenReturn(6);

            String message = turnService.drawCard(1L);

            assertThat(message).contains("FLIP 7");
            assertThat(roundPlayer.getStatus()).isEqualTo(RoundPlayerStatus.STANDING);
            assertThat(roundPlayer.getRoundPoints()).isEqualTo(36);
            }

    private Game createGame() {
        Game game = new Game();
        game.setId(1L);

        Player current = new Player();
        current.setId(1L);
        current.setName("Alice");
        current.setGame(game);

        Player p2 = new Player();
        p2.setId(2L);
        p2.setName("Bob");
        p2.setGame(game);

        Player p3 = new Player();
        p3.setId(3L);
        p3.setName("Charlie");
        p3.setGame(game);

        Player p4 = new Player();
        p4.setId(4L);
        p4.setName("Diana");
        p4.setGame(game);

        game.setPlayers(List.of(current, p2, p3, p4));
        game.setCurrentRound(1);
        game.setCurrentPlayerTurnIndex(0);
        return game;
    }

    private RoundPlayer activeRoundPlayer(Game game, List<Integer> cards) {
        RoundPlayer roundPlayer = new RoundPlayer();
        roundPlayer.setGame(game);
        roundPlayer.setPlayer(game.getPlayers().get(0));
        roundPlayer.setRoundNumber(game.getCurrentRound());
        roundPlayer.setStatus(RoundPlayerStatus.ACTIVE);
        roundPlayer.setCurrentCards(cards);
        return roundPlayer;
    }
}
