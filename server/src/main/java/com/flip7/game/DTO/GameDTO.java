package com.flip7.game.DTO;

import com.flip7.game.GameStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class GameDTO {
    private Long id;
    private GameStatus status;
    private int currentRound;
    private List<PlayerDTO> players;
}
