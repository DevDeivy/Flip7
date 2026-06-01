package com.flip7.game.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class RoomStateDTO {
    private Long roomId;
    private String code;
    private String status;
    private String hostName;
    private int currentPlayers;
    private int minimumPlayersToStart;
    private Long gameId;
    private List<RoomParticipantDTO> participants;
}
