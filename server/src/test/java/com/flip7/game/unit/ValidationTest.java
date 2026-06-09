package com.flip7.game.unit;

import com.flip7.game.DTO.CreateGameDTO;
import com.flip7.game.repository.DeckRepository;
import com.flip7.game.repository.GameRepository;
import com.flip7.game.repository.PlayerRepository;
import com.flip7.game.repository.RoundPlayerRepository;
import com.flip7.game.service.DeckService;
import com.flip7.game.service.GameService;
import com.flip7.game.service.PlayerService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ValidationTest {

    private final GameService gameService = new GameService(
            mock(GameRepository.class),
            mock(PlayerService.class),
            mock(PlayerRepository.class),
            mock(DeckService.class),
            mock(DeckRepository.class),
            mock(RoundPlayerRepository.class)
    );

    @Test
    void createGame_failsWithLessThan4Players() {
        CreateGameDTO dto = new CreateGameDTO();
        dto.setPlayers(List.of("A", "B", "C"));

        assertThatThrownBy(() -> gameService.createGame(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entre 4 y 8");
    }

    @Test
    void createGame_failsWithMoreThan8Players() {
        CreateGameDTO dto = new CreateGameDTO();
        dto.setPlayers(List.of("A", "B", "C", "D", "E", "F", "G", "H", "I"));

        assertThatThrownBy(() -> gameService.createGame(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entre 4 y 8");
    }
}
