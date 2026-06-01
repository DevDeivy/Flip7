package com.flip7.game.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateRoomDTO {
    @NotBlank(message = "El nombre del host es obligatorio")
    private String hostName;
}
