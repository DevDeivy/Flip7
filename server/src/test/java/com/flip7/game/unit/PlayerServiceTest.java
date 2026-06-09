package com.flip7.game.unit;

import com.flip7.game.DTO.PlayerDTO;
import com.flip7.game.model.Game;
import com.flip7.game.model.Player;
import com.flip7.game.repository.PlayerRepository;
import com.flip7.game.service.PlayerService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlayerServiceTest {

    @Test
    void createPlayers_marksOnlyLastAsAi_whenEnabled() {
        PlayerRepository repository = mock(PlayerRepository.class);
        PlayerService service = new PlayerService(repository);
        Game game = new Game();

        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Player> players = service.createPlayers(List.of("A", "B", "C", "D"), game, true);

        assertThat(players).hasSize(4);
        assertThat(players.get(0).isAiControlled()).isFalse();
        assertThat(players.get(1).isAiControlled()).isFalse();
        assertThat(players.get(2).isAiControlled()).isFalse();
        assertThat(players.get(3).isAiControlled()).isTrue();
    }

    @Test
    void getPlayersByGame_mapsToDto() {
        PlayerRepository repository = mock(PlayerRepository.class);
        PlayerService service = new PlayerService(repository);

        Player player = new Player();
        player.setId(11L);
        player.setName("Alice");
        player.setTotalPoints(42);

        when(repository.findByGameId(7L)).thenReturn(List.of(player));

        List<PlayerDTO> dtos = service.getPlayersByGame(7L);

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).getId()).isEqualTo(11L);
        assertThat(dtos.get(0).getName()).isEqualTo("Alice");
        assertThat(dtos.get(0).getTotalPoints()).isEqualTo(42);
    }

    @Test
    void addPoints_updatesExistingPlayer() {
        PlayerRepository repository = mock(PlayerRepository.class);
        PlayerService service = new PlayerService(repository);

        Player player = new Player();
        player.setId(3L);
        player.setTotalPoints(10);

        when(repository.findById(3L)).thenReturn(Optional.of(player));
        when(repository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.addPoints(3L, 15);

        assertThat(player.getTotalPoints()).isEqualTo(25);
    }

    @Test
    void addPoints_throwsWhenPlayerDoesNotExist() {
        PlayerRepository repository = mock(PlayerRepository.class);
        PlayerService service = new PlayerService(repository);

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addPoints(99L, 5))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Jugador no encontrado");
    }
}
