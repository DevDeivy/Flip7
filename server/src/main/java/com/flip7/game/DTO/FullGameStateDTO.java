package com.flip7.game.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import com.flip7.game.DTO.PlayerDTO;

@Data
@NoArgsConstructor
public class FullGameStateDTO {
    private Long gameId;
    private String status;
    private int currentRound;
    private int currentPlayerTurnIndex;
    private Long currentPlayerTurnId;
    private int startingPlayerIndex;
    private List<FullPlayerStateDTO> players;
    private List<PlayerDTO> scoreboard;
    private int deckRemaining;
    private String lastMessage;
    private PlayerDTO winner;
}
