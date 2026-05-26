package com.flip7.game.service;

import com.flip7.game.model.Deck;
import com.flip7.game.model.Game;
import com.flip7.game.repository.DeckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class DeckService {

    private final DeckRepository deckRepository;

    public Deck createDeck(Game game) {
        Deck deck = new Deck();
        deck.setGame(game);
        return deckRepository.save(deck);
    }

    public boolean isDeckEmpty(Game game) {
        Deck deck = deckRepository.findByGameId(game.getId())
                .orElseThrow(() -> new RuntimeException("Mazo no encontrado"));
        return deck.isEmpty();
    }

    public int drawCard(Game game) {
        Deck deck = deckRepository.findByGameId(game.getId())
                .orElseThrow(() -> new RuntimeException("Mazo no encontrado"));

        if (deck.isEmpty()) {
            deck.reset();
        }

        List<Integer> available = deck.getAvailableCards();
        int card = available.get(ThreadLocalRandom.current().nextInt(available.size()));
        deck.decrementCount(card);
        deckRepository.save(deck);
        return card;
    }
}
