package com.flip7.game.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.antlr.v4.runtime.misc.NotNull;

import java.util.List;

public class CreateGameDTO {
    @NotNull
    @Size(min = 4, max = 8, message = "Debe haber entre 4 y 8 jugadores")
    private List<@NotBlank(message = "El nombre no puede estar vacío") String> players;

    public List<String> getPlayers() { return players; }
    public void setPlayers(List<String> players) { this.players = players; }
}
