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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScoreCalculationTest {

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
    void duplicateCard_eliminatesPlayerAndResetsRoundPoints() {
        Game game = createGame();
        RoundPlayer rp = roundPlayer(game, game.getPlayers().get(0), List.of(5));

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(roundPlayerRepository.findByPlayerIdAndGameIdAndRoundNumber(eq(1L), eq(1L), eq(1))).thenReturn(Optional.of(rp));
        when(roundPlayerRepository.findByGameIdAndRoundNumber(eq(1L), eq(1))).thenReturn(List.of(rp));
        when(deckService.drawCard(game)).thenReturn(5);

        String result = turnService.drawCard(1L);

        assertThat(result).contains("eliminado");
        assertThat(game.getLastDuplicateCard()).isEqualTo(5);
        assertThat(game.getLastDuplicatePlayerId()).isEqualTo(1L);
        assertThat(rp.getStatus()).isEqualTo(RoundPlayerStatus.ELIMINATED);
        assertThat(rp.getRoundPoints()).isZero();
    }

    @Test
    void sevenUniqueCards_appliesFlip7BonusOf15() {
        Game game = createGame();
        RoundPlayer rp = roundPlayer(game, game.getPlayers().get(0), List.of(0, 1, 2, 3, 4, 5));

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(roundPlayerRepository.findByPlayerIdAndGameIdAndRoundNumber(eq(1L), eq(1L), eq(1))).thenReturn(Optional.of(rp));
        when(roundPlayerRepository.findByGameIdAndRoundNumber(eq(1L), eq(1))).thenReturn(List.of(rp));
        when(deckService.drawCard(game)).thenReturn(6);

        String result = turnService.drawCard(1L);

        assertThat(result).contains("FLIP 7");
        assertThat(rp.getStatus()).isEqualTo(RoundPlayerStatus.STANDING);
        assertThat(rp.getRoundPoints()).isEqualTo(36);
    }

    @Test
    void stand_keepsCurrentRoundScore_whenPlayerStands() {
        Game game = createGame();
        RoundPlayer rp = roundPlayer(game, game.getPlayers().get(0), List.of(4, 7));

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(roundPlayerRepository.findByPlayerIdAndGameIdAndRoundNumber(eq(1L), eq(1L), eq(1))).thenReturn(Optional.of(rp));
        when(roundPlayerRepository.findByGameIdAndRoundNumber(eq(1L), eq(1))).thenReturn(List.of(rp));

        String result = turnService.stand(1L);

        assertThat(result).contains("plantado");
        assertThat(rp.getStatus()).isEqualTo(RoundPlayerStatus.STANDING);
        assertThat(rp.getRoundPoints()).isEqualTo(11);
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
        RoundPlayer rp = new RoundPlayer();
        rp.setGame(game);
        rp.setPlayer(player);
        rp.setRoundNumber(game.getCurrentRound());
        rp.setCurrentCards(new ArrayList<>(cards));
        rp.setStatus(RoundPlayerStatus.ACTIVE);
        return rp;
    }
}
