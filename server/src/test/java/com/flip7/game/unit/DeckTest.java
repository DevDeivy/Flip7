package com.flip7.game.unit;

import com.flip7.game.model.Deck;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeckTest {

    private static void clearDeck(Deck deck) {
        deck.setCount0(0);
        deck.setCount1(0);
        deck.setCount2(0);
        deck.setCount3(0);
        deck.setCount4(0);
        deck.setCount5(0);
        deck.setCount6(0);
        deck.setCount7(0);
        deck.setCount8(0);
        deck.setCount9(0);
        deck.setCount10(0);
        deck.setCount11(0);
        deck.setCount12(0);
        deck.setCountFreeze(0);
        deck.setCountFlipThree(0);
        deck.setCountSecondChance(0);
        deck.setCountX2(0);
        deck.setCountPlus2(0);
        deck.setCountPlus4(0);
        deck.setCountPlus6(0);
        deck.setCountPlus8(0);
        deck.setCountPlus10(0);
    }

    @Test
    void deckComposition_matchesFlip7Rules() {
        Deck deck = new Deck();
        deck.reset();

        List<Integer> cards = deck.getAvailableCards();

        assertThat(cards.stream().filter(c -> c == 0).count()).isEqualTo(1);
        assertThat(cards.stream().filter(c -> c == 1).count()).isEqualTo(1);
        assertThat(cards.stream().filter(c -> c == 2).count()).isEqualTo(2);
        assertThat(cards.stream().filter(c -> c == 3).count()).isEqualTo(3);
        assertThat(cards.stream().filter(c -> c == 12).count()).isEqualTo(12);

        assertThat(cards).contains(100, 101, 102, 200, 201, 202, 203, 204, 205);
    }

    @Test
    void reset_refillsDeckAfterBeingEmptied() {
        Deck deck = new Deck();
        clearDeck(deck);

        assertThat(deck.isEmpty()).isTrue();

        deck.reset();

        assertThat(deck.isEmpty()).isFalse();
        assertThat(deck.getAvailableCards()).isNotEmpty();
    }

    @Test
    void getAvailableCards_respectsExactMultiplicityForAllNumericCards() {
        Deck deck = new Deck();
        clearDeck(deck);

        deck.setCount2(1);
        deck.setCount3(2);
        deck.setCount4(3);
        deck.setCount5(4);
        deck.setCount6(5);
        deck.setCount7(6);
        deck.setCount8(7);
        deck.setCount9(8);
        deck.setCount10(9);
        deck.setCount11(10);
        deck.setCount12(11);

        List<Integer> cards = deck.getAvailableCards();

        assertThat(cards).doesNotContain(0, 1, 100, 101, 102, 200, 201, 202, 203, 204, 205);
        assertThat(cards.stream().filter(c -> c == 2).count()).isEqualTo(1);
        assertThat(cards.stream().filter(c -> c == 3).count()).isEqualTo(2);
        assertThat(cards.stream().filter(c -> c == 4).count()).isEqualTo(3);
        assertThat(cards.stream().filter(c -> c == 5).count()).isEqualTo(4);
        assertThat(cards.stream().filter(c -> c == 6).count()).isEqualTo(5);
        assertThat(cards.stream().filter(c -> c == 7).count()).isEqualTo(6);
        assertThat(cards.stream().filter(c -> c == 8).count()).isEqualTo(7);
        assertThat(cards.stream().filter(c -> c == 9).count()).isEqualTo(8);
        assertThat(cards.stream().filter(c -> c == 10).count()).isEqualTo(9);
        assertThat(cards.stream().filter(c -> c == 11).count()).isEqualTo(10);
        assertThat(cards.stream().filter(c -> c == 12).count()).isEqualTo(11);
    }

    @Test
    void getAvailableCards_returnsEmpty_whenAllCountsAreZero() {
        Deck deck = new Deck();
        clearDeck(deck);

        assertThat(deck.isEmpty()).isTrue();
        assertThat(deck.getAvailableCards()).isEmpty();
    }

    @Test
    void isEmpty_isFalse_whenAnySinglePileHasCards() {
        Deck deck = new Deck();
        clearDeck(deck);

        deck.setCount0(1);
        assertThat(deck.isEmpty()).isFalse();
        clearDeck(deck);

        deck.setCount7(1);
        assertThat(deck.isEmpty()).isFalse();
        clearDeck(deck);

        deck.setCount12(1);
        assertThat(deck.isEmpty()).isFalse();
        clearDeck(deck);

        deck.setCountFreeze(1);
        assertThat(deck.isEmpty()).isFalse();
        clearDeck(deck);

        deck.setCountSecondChance(1);
        assertThat(deck.isEmpty()).isFalse();
        clearDeck(deck);

        deck.setCountX2(1);
        assertThat(deck.isEmpty()).isFalse();
        clearDeck(deck);

        deck.setCountPlus10(1);
        assertThat(deck.isEmpty()).isFalse();
    }

    @Test
    void decrementCount_decrementsEverySupportedCardType() {
        Deck deck = new Deck();
        deck.reset();

        deck.decrementCount(0);
        deck.decrementCount(1);
        deck.decrementCount(2);
        deck.decrementCount(3);
        deck.decrementCount(4);
        deck.decrementCount(5);
        deck.decrementCount(6);
        deck.decrementCount(7);
        deck.decrementCount(8);
        deck.decrementCount(9);
        deck.decrementCount(10);
        deck.decrementCount(11);
        deck.decrementCount(12);
        deck.decrementCount(100);
        deck.decrementCount(101);
        deck.decrementCount(102);
        deck.decrementCount(200);
        deck.decrementCount(201);
        deck.decrementCount(202);
        deck.decrementCount(203);
        deck.decrementCount(204);
        deck.decrementCount(205);

        assertThat(deck.getCount0()).isZero();
        assertThat(deck.getCount1()).isZero();
        assertThat(deck.getCount2()).isEqualTo(1);
        assertThat(deck.getCount3()).isEqualTo(2);
        assertThat(deck.getCount4()).isEqualTo(3);
        assertThat(deck.getCount5()).isEqualTo(4);
        assertThat(deck.getCount6()).isEqualTo(5);
        assertThat(deck.getCount7()).isEqualTo(6);
        assertThat(deck.getCount8()).isEqualTo(7);
        assertThat(deck.getCount9()).isEqualTo(8);
        assertThat(deck.getCount10()).isEqualTo(9);
        assertThat(deck.getCount11()).isEqualTo(10);
        assertThat(deck.getCount12()).isEqualTo(11);
        assertThat(deck.getCountFreeze()).isEqualTo(2);
        assertThat(deck.getCountFlipThree()).isEqualTo(2);
        assertThat(deck.getCountSecondChance()).isEqualTo(1);
        assertThat(deck.getCountX2()).isZero();
        assertThat(deck.getCountPlus2()).isZero();
        assertThat(deck.getCountPlus4()).isZero();
        assertThat(deck.getCountPlus6()).isZero();
        assertThat(deck.getCountPlus8()).isZero();
        assertThat(deck.getCountPlus10()).isZero();
    }
}
