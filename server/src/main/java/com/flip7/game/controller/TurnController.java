package com.flip7.game.controller;

import com.flip7.game.DTO.FullGameStateDTO;
import com.flip7.game.service.GameService;
import com.flip7.game.service.TurnService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/flip/game", "/api/flip/games"})
@RequiredArgsConstructor
public class TurnController {

    private final TurnService turnService;
    private final GameService gameService;

    @PostMapping("/{gameId}/draw")
    public ResponseEntity<FullGameStateDTO> drawCard(@PathVariable Long gameId) {
        String message = turnService.drawCard(gameId);
        FullGameStateDTO state = gameService.getFullState(gameId);
        state.setLastMessage(message);
        return ResponseEntity.ok(state);
    }

    @PostMapping("/{gameId}/stand")
    public ResponseEntity<FullGameStateDTO> stand(@PathVariable Long gameId) {
        String message = turnService.stand(gameId);
        FullGameStateDTO state = gameService.getFullState(gameId);
        state.setLastMessage(message);
        return ResponseEntity.ok(state);
    }
}
