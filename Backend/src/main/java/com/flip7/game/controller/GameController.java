package com.flip7.game.controller;

import com.flip7.game.DTO.CreateGameDTO;
import com.flip7.game.DTO.GameDTO;
import com.flip7.game.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/flip/game")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @PostMapping
    public ResponseEntity<GameDTO> createGame(@RequestBody @Valid CreateGameDTO request) {
        return ResponseEntity.ok(gameService.createGame(request));
    }
}
