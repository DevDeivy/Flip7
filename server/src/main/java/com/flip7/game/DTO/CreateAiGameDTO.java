package com.flip7.game.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateAiGameDTO {
    @NotBlank(message = "El nombre del jugador es obligatorio")
    private String playerName;
}