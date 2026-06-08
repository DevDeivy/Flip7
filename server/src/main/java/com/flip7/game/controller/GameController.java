package com.flip7.game.controller;

import com.flip7.game.DTO.CreateGameDTO;
import com.flip7.game.DTO.CreateAiGameDTO;
import com.flip7.game.DTO.FullGameStateDTO;
import com.flip7.game.DTO.PlayerDTO;
import com.flip7.game.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/flip/game", "/api/flip/games"})
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @PostMapping
    public ResponseEntity<FullGameStateDTO> createGame(@RequestBody @Valid CreateGameDTO request) {
        FullGameStateDTO state = gameService.createGame(request);
        return ResponseEntity.ok(state);
    }

    @PostMapping("/vs-ai")
    public ResponseEntity<FullGameStateDTO> createAiGame(@RequestBody @Valid CreateAiGameDTO request) {
        return ResponseEntity.ok(gameService.createAiGame(request.getPlayerName()));
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<FullGameStateDTO> getGame(@PathVariable Long gameId) {
        return ResponseEntity.ok(gameService.getFullState(gameId));
    }

    @GetMapping("/{gameId}/state")
    public ResponseEntity<FullGameStateDTO> getState(@PathVariable Long gameId) {
        return ResponseEntity.ok(gameService.getFullState(gameId));
    }

    @PostMapping("/{gameId}/start")
    public ResponseEntity<FullGameStateDTO> startGame(@PathVariable Long gameId) {
        return ResponseEntity.ok(gameService.getFullState(gameId));
    }

    @GetMapping("/{gameId}/scoreboard")
    public ResponseEntity<List<PlayerDTO>> getScoreboard(@PathVariable Long gameId) {
        return ResponseEntity.ok(gameService.getScoreboard(gameId));
    }

    @GetMapping("/{gameId}/winner")
    public ResponseEntity<PlayerDTO> getWinner(@PathVariable Long gameId) {
        PlayerDTO winner = gameService.getWinner(gameId);
        if (winner == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(winner);
    }
}
