package com.flip7.game.DTO;

import com.flip7.game.RoundPlayerStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class DrawResultDTO {
    private String playerName;
    private int cardDrawn;
    private String cardType;
    private List<Integer> currentCards;
    private int roundPoints;
    private RoundPlayerStatus status;
    private String message;
}
