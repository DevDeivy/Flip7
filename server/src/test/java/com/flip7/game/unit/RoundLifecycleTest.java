package com.flip7.game.unit;

import com.flip7.game.GameStatus;
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

class RoundLifecycleTest {

    @Test
    void allStanding_finishesRoundAndRotatesStartingPlayer() {
        RoundPlayerRepository roundPlayerRepository = mock(RoundPlayerRepository.class);
        GameRepository gameRepository = mock(GameRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        DeckService deckService = mock(DeckService.class);

        TurnService turnService = new TurnService(
                roundPlayerRepository,
                playerRepository,
                gameRepository,
                deckService,
                mock(OllamaAiService.class),
                mock(TransactionTemplate.class)
        );

        Game game = createGame();
        RoundPlayer current = roundPlayer(game, game.getPlayers().get(0), RoundPlayerStatus.ACTIVE, List.of(5));
        RoundPlayer p2 = roundPlayer(game, game.getPlayers().get(1), RoundPlayerStatus.STANDING, List.of(8));
        RoundPlayer p3 = roundPlayer(game, game.getPlayers().get(2), RoundPlayerStatus.STANDING, List.of(7));
        RoundPlayer p4 = roundPlayer(game, game.getPlayers().get(3), RoundPlayerStatus.STANDING, List.of(4));

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roundPlayerRepository.save(any(RoundPlayer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roundPlayerRepository.findByPlayerIdAndGameIdAndRoundNumber(eq(1L), eq(1L), eq(1))).thenReturn(Optional.of(current));
        when(roundPlayerRepository.findByGameIdAndRoundNumber(eq(1L), eq(1))).thenReturn(List.of(current, p2, p3, p4));

        String message = turnService.stand(1L);

        assertThat(message).contains("plantado");
        assertThat(game.getCurrentRound()).isEqualTo(2);
        assertThat(game.getStartingPlayerIndex()).isEqualTo(1);
        assertThat(game.getCurrentPlayerTurnIndex()).isEqualTo(1);
        assertThat(game.getGameStatus()).isEqualTo(GameStatus.PLAYING);
    }

    private Game createGame() {
        Game game = new Game();
        game.setId(1L);
        game.setCurrentRound(1);
        game.setCurrentPlayerTurnIndex(0);
        game.setStartingPlayerIndex(0);
        game.setGameStatus(GameStatus.PLAYING);

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

    private RoundPlayer roundPlayer(Game game, Player player, RoundPlayerStatus status, List<Integer> cards) {
        RoundPlayer rp = new RoundPlayer();
        rp.setGame(game);
        rp.setPlayer(player);
        rp.setRoundNumber(game.getCurrentRound());
        rp.setStatus(status);
        rp.setCurrentCards(new ArrayList<>(cards));
        return rp;
    }
}
