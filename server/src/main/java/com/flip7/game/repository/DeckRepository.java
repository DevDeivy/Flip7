package com.flip7.game.repository;

import com.flip7.game.model.Deck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeckRepository extends JpaRepository<Deck, Long> {
    Optional<Deck> findByGameId(Long gameId);
}
