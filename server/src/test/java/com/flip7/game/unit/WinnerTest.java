package com.flip7.game.unit;

import com.flip7.game.model.Game;
import com.flip7.game.model.Player;
import com.flip7.game.repository.DeckRepository;
import com.flip7.game.repository.GameRepository;
import com.flip7.game.repository.PlayerRepository;
import com.flip7.game.repository.RoundPlayerRepository;
import com.flip7.game.service.DeckService;
import com.flip7.game.service.GameService;
import com.flip7.game.service.PlayerService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WinnerTest {

    private final GameService gameService = new GameService(
            mock(GameRepository.class),
            mock(PlayerService.class),
            mock(PlayerRepository.class),
            mock(DeckService.class),
            mock(DeckRepository.class),
            mock(RoundPlayerRepository.class)
    );

    @Test
    void hasWinner_isTrueWhenPlayerReaches200() {
        Game game = gameWithScores(199, 200, 120, 90);

        assertThat(gameService.hasWinner(game)).isTrue();
    }

    @Test
    void hasWinner_isFalseWhenAllBelow200() {
        Game game = gameWithScores(199, 150, 120, 90);

        assertThat(gameService.hasWinner(game)).isFalse();
    }

    @Test
    void getWinner_returnsPlayerWithHighestScore() {
        Game game = gameWithScores(50, 120, 210, 80);

        Player winner = gameService.getWinner(game);

        assertThat(winner.getTotalPoints()).isEqualTo(210);
    }

    private Game gameWithScores(int... scores) {
        Game game = new Game();
        List<Player> players = java.util.Arrays.stream(scores)
                .mapToObj(score -> {
                    Player player = new Player();
                    player.setTotalPoints(score);
                    player.setGame(game);
                    return player;
                })
                .toList();
        game.setPlayers(players);
        return game;
    }
}
