package com.flip7.game.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateAlertDTO {
    private String playerId;
    private String playerName;
    private int cardValue;
    private String message;
}
