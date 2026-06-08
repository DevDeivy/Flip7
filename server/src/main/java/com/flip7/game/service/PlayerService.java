package com.flip7.game.service;

import com.flip7.game.DTO.PlayerDTO;
import com.flip7.game.model.Game;
import com.flip7.game.model.Player;
import com.flip7.game.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class PlayerService {
    private final PlayerRepository playerRepository;

    public List<Player> createPlayers(List<String> names, Game game) {
        return createPlayers(names, game, false);
    }

    public List<Player> createPlayers(List<String> names, Game game, boolean markLastPlayerAsAi) {
        List<Player> players = IntStream.range(0, names.size())
                .mapToObj(index -> {
                    String name = names.get(index);
                    Player player = new Player();
                    player.setName(name);
                    player.setTotalPoints(0);
                    player.setAiControlled(markLastPlayerAsAi && index == names.size() - 1);
                    player.setGame(game);
                    return player;
                })
                .collect(Collectors.toList());

        return playerRepository.saveAll(players);
    }

    public List<PlayerDTO> getPlayersByGame(Long gameId) {
        return playerRepository.findByGameId(gameId)
                .stream()
                .map(player -> {
                    PlayerDTO dto = new PlayerDTO();
                    dto.setId(player.getId());
                    dto.setName(player.getName());
                    dto.setTotalPoints(player.getTotalPoints());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public void addPoints(Long playerId, int points) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Jugador no encontrado"));
        player.setTotalPoints(player.getTotalPoints() + points);
        playerRepository.save(player);
    }
}
