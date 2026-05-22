package com.flip7.game.controller;

import com.flip7.game.service.GetCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/flip")
@RequiredArgsConstructor
public class GetCardController {

    private final GetCardService getCardService;

    @GetMapping("/")
    public ResponseEntity<String> getCardtoPlayer(){
        return ResponseEntity.ok("your number card is: " + getCardService.generateCard());
    }

    @GetMapping("/stand")
    public ResponseEntity<String> getPoints(){
        return ResponseEntity.ok("total points = " + getCardService.getPoints());
    }
}
