package com.flip7.game.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StandResultDTO {
    private String playerName;
    private int roundPoints;
    private int totalPoints;
    private String message;
}
