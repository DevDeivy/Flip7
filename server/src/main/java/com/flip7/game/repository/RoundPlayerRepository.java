package com.flip7.game.repository;

import com.flip7.game.model.RoundPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoundPlayerRepository extends JpaRepository<RoundPlayer, Long> {
    List<RoundPlayer> findByGameIdAndRoundNumber(Long gameId, int roundNumber);
    Optional<RoundPlayer> findByPlayerIdAndGameIdAndRoundNumber(Long playerId, Long gameId, int roundNumber);
}
