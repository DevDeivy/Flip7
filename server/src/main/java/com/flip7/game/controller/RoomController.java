package com.flip7.game.controller;

import com.flip7.game.DTO.CreateRoomDTO;
import com.flip7.game.DTO.FullGameStateDTO;
import com.flip7.game.DTO.JoinRoomDTO;
import com.flip7.game.DTO.RoomStateDTO;
import com.flip7.game.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/flip/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomStateDTO> createRoom(@RequestBody @Valid CreateRoomDTO request) {
        return ResponseEntity.ok(roomService.createRoom(request.getHostName()));
    }

    @PostMapping("/{code}/join")
    public ResponseEntity<RoomStateDTO> joinRoom(@PathVariable String code, @RequestBody @Valid JoinRoomDTO request) {
        return ResponseEntity.ok(roomService.joinRoom(code, request.getPlayerName()));
    }

    @GetMapping("/{code}")
    public ResponseEntity<RoomStateDTO> getRoom(@PathVariable String code) {
        return ResponseEntity.ok(roomService.getRoomState(code));
    }

    @PostMapping("/{code}/start")
    public ResponseEntity<FullGameStateDTO> startRoom(@PathVariable String code) {
        return ResponseEntity.ok(roomService.startRoom(code));
    }
}
