package com.flip7.game.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class FullGameStateDTO {
    private Long gameId;
    private String status;
    private int currentRound;
    private int currentPlayerTurnIndex;
    private int startingPlayerIndex;
    private List<FullPlayerStateDTO> players;
    private int deckRemaining;
    private String lastMessage;
}
