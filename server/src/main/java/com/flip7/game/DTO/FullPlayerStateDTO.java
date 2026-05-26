package com.flip7.game.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class FullPlayerStateDTO {
    private Long playerId;
    private String name;
    private int totalPoints;
    private List<Integer> roundCards;
    private String status;
    private boolean hasSecondChance;
    private List<Integer> modifierCardValues;
    private int roundPoints;
}
