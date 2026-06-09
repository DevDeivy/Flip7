package com.flip7.game.testutils;

import com.flip7.game.model.Game;
import com.flip7.game.repository.DeckRepository;
import com.flip7.game.service.DeckService;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;

public class DeterministicDeckService extends DeckService {

    private final Queue<Integer> cards = new ArrayDeque<>();

    public DeterministicDeckService(DeckRepository deckRepository) {
        super(deckRepository);
    }

    public void setCards(Collection<Integer> nextCards) {
        cards.clear();
        cards.addAll(nextCards);
    }

    @Override
    public int drawCard(Game game) {
        Integer next = cards.poll();
        if (next == null) {
            throw new IllegalStateException("No hay cartas configuradas para test");
        }
        return next;
    }

    @Override
    public boolean isDeckEmpty(Game game) {
        return cards.isEmpty();
    }
}
