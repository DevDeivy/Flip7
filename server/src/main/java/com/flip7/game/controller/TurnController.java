package com.flip7.game.controller;

import com.flip7.game.DTO.DrawResultDTO;
import com.flip7.game.DTO.StandResultDTO;
import com.flip7.game.service.TurnService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/flip/game")
@RequiredArgsConstructor
public class TurnController {

    private final TurnService turnService;

    @PostMapping("/{gameId}/draw")
    public ResponseEntity<DrawResultDTO> drawCard(@PathVariable Long gameId) {
        return ResponseEntity.ok(turnService.drawCard(gameId));
    }

    @PostMapping("/{gameId}/stand")
    public ResponseEntity<StandResultDTO> stand(@PathVariable Long gameId) {
        return ResponseEntity.ok(turnService.stand(gameId));
    }
}
