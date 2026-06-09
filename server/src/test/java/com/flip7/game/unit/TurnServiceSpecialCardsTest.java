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

class TurnServiceSpecialCardsTest {

    private RoundPlayerRepository roundPlayerRepository;
    private GameRepository gameRepository;
    private DeckService deckService;
    private TurnService turnService;

    @BeforeEach
    void setUp() {
        roundPlayerRepository = mock(RoundPlayerRepository.class);
        gameRepository = mock(GameRepository.class);
        deckService = mock(DeckService.class);

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
    }

    @Test
    void drawCard_freezeCard_setsPlayerStanding() {
        Game game = createGame();
        RoundPlayer roundPlayer = roundPlayer(game, game.getPlayers().get(0), List.of(5));

        wireCommon(game, roundPlayer);
        when(deckService.drawCard(game)).thenReturn(100);

        String message = turnService.drawCard(1L);

        assertThat(message).contains("FREEZE");
        assertThat(roundPlayer.getStatus()).isEqualTo(RoundPlayerStatus.STANDING);
        assertThat(roundPlayer.getRoundPoints()).isEqualTo(5);
        assertThat(game.getLastMessage()).contains("FREEZE");
        assertThat(game.getCurrentPlayerTurnIndex()).isEqualTo(1);
    }

    @Test
    void drawCard_flipThreeCard_setsSpecialMessage() {
        Game game = createGame();
        RoundPlayer roundPlayer = roundPlayer(game, game.getPlayers().get(0), List.of(2));

        wireCommon(game, roundPlayer);
        when(deckService.drawCard(game)).thenReturn(101);

        String message = turnService.drawCard(1L);

        assertThat(message).contains("FLIP THREE");
        assertThat(game.getLastMessage()).contains("FLIP THREE");
        assertThat(game.getCurrentPlayerTurnIndex()).isEqualTo(1);
    }

    @Test
    void drawCard_secondChanceCard_grantsProtection() {
        Game game = createGame();
        RoundPlayer roundPlayer = roundPlayer(game, game.getPlayers().get(0), List.of(2));

        wireCommon(game, roundPlayer);
        when(deckService.drawCard(game)).thenReturn(102);

        String message = turnService.drawCard(1L);

        assertThat(message).contains("Segunda Oportunidad");
        assertThat(roundPlayer.isHasSecondChance()).isTrue();
        assertThat(game.getLastMessage()).contains("Segunda Oportunidad");
        assertThat(game.getCurrentPlayerTurnIndex()).isEqualTo(1);
    }

    @Test
    void drawCard_secondChanceCard_whenAlreadyHasOne_discardsNewCard() {
        Game game = createGame();
        RoundPlayer roundPlayer = roundPlayer(game, game.getPlayers().get(0), List.of(2));
        roundPlayer.setHasSecondChance(true);

        wireCommon(game, roundPlayer);
        when(deckService.drawCard(game)).thenReturn(102);

        String message = turnService.drawCard(1L);

        assertThat(message).contains("ya tenía");
        assertThat(roundPlayer.isHasSecondChance()).isTrue();
        assertThat(game.getLastMessage()).contains("ya tenía");
        assertThat(game.getCurrentPlayerTurnIndex()).isEqualTo(1);
    }

    @Test
    void drawCard_modifierCards_applyEffects() {
        Game game = createGame();
        RoundPlayer roundPlayer = roundPlayer(game, game.getPlayers().get(0), List.of(4));

        wireCommon(game, roundPlayer);
        when(deckService.drawCard(game)).thenReturn(200, 203);

        String first = turnService.drawCard(1L);
        game.setCurrentPlayerTurnIndex(0);
        String second = turnService.drawCard(1L);

        assertThat(first).contains("modificador x2");
        assertThat(second).contains("modificador +6");
        assertThat(roundPlayer.isHasX2Multiplier()).isTrue();
        assertThat(roundPlayer.getModifierBonus()).isEqualTo(6);
        assertThat(roundPlayer.getModifierCardValues()).containsExactly(200, 203);
        assertThat(game.getLastMessage()).contains("modificador +6");
    }

    @Test
    void drawCard_freeze_whenRoundEnds_clearsRoundAlertsAndAdvancesRound() {
        Game game = createGame();
        game.setLastDuplicateCard(9);
        game.setLastDuplicatePlayerId(1L);
        game.setAiReason("old");

        RoundPlayer current = roundPlayer(game, game.getPlayers().get(0), List.of(5));
        RoundPlayer p2 = roundPlayer(game, game.getPlayers().get(1), List.of(3));
        RoundPlayer p3 = roundPlayer(game, game.getPlayers().get(2), List.of(4));
        RoundPlayer p4 = roundPlayer(game, game.getPlayers().get(3), List.of(6));
        p2.setStatus(RoundPlayerStatus.STANDING);
        p3.setStatus(RoundPlayerStatus.STANDING);
        p4.setStatus(RoundPlayerStatus.STANDING);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(roundPlayerRepository.findByPlayerIdAndGameIdAndRoundNumber(eq(1L), eq(1L), eq(1)))
                .thenReturn(Optional.of(current));
        when(roundPlayerRepository.findByGameIdAndRoundNumber(eq(1L), eq(1)))
                .thenReturn(List.of(current, p2, p3, p4));
        when(deckService.drawCard(game)).thenReturn(100);

        turnService.drawCard(1L);

        assertThat(game.getCurrentRound()).isEqualTo(2);
        assertThat(game.getStartingPlayerIndex()).isEqualTo(1);
        assertThat(game.getCurrentPlayerTurnIndex()).isEqualTo(1);
        assertThat(game.getLastDuplicateCard()).isNull();
        assertThat(game.getLastDuplicatePlayerId()).isNull();
        assertThat(game.getAiReason()).isNull();
    }

    @Test
    void drawCard_unknownCard_returnsUnknownMessage() {
        Game game = createGame();
        RoundPlayer roundPlayer = roundPlayer(game, game.getPlayers().get(0), List.of(1));

        wireCommon(game, roundPlayer);
        when(deckService.drawCard(game)).thenReturn(150);

        String message = turnService.drawCard(1L);

        assertThat(message).isEqualTo("Carta desconocida.");
    }

    @Test
    void drawCard_failsWhenPlayerIsNotActive() {
        Game game = createGame();
        RoundPlayer roundPlayer = roundPlayer(game, game.getPlayers().get(0), List.of(1));
        roundPlayer.setStatus(RoundPlayerStatus.STANDING);

        wireCommon(game, roundPlayer);

        assertThatThrownBy(() -> turnService.drawCard(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ya no puede pedir cartas");
    }

    private void wireCommon(Game game, RoundPlayer roundPlayer) {
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(roundPlayerRepository.findByPlayerIdAndGameIdAndRoundNumber(eq(1L), eq(1L), eq(1)))
                .thenReturn(Optional.of(roundPlayer));
        when(roundPlayerRepository.findByGameIdAndRoundNumber(eq(1L), eq(1)))
                .thenReturn(List.of(roundPlayer));
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

    private Player player(Long id, String name, Game game) {
        Player player = new Player();
        player.setId(id);
        player.setName(name);
        player.setGame(game);
        return player;
    }

    private RoundPlayer roundPlayer(Game game, Player player, List<Integer> cards) {
        RoundPlayer roundPlayer = new RoundPlayer();
        roundPlayer.setGame(game);
        roundPlayer.setPlayer(player);
        roundPlayer.setRoundNumber(1);
        roundPlayer.setCurrentCards(new ArrayList<>(cards));
        return roundPlayer;
    }
}
