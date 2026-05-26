package com.flip7.game.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeckTest {

    private static final int INITIAL_SIZE = 88;

    private Deck deck;

    @BeforeEach
    void setUp() {
        deck = new Deck();
        deck.reset();
    }

    @Test
    void reset_setsAllCountsToInitialValues() {
        assertThat(deck.isEmpty()).isFalse();
        assertThat(deck.getAvailableCards()).hasSize(INITIAL_SIZE);
    }

    @Test
    void isEmpty_returnsTrue_whenAllCountsZero() {
        zeroOutAllCounts(deck);
        assertThat(deck.isEmpty()).isTrue();
    }

    @Test
    void isEmpty_returnsFalse_whenNumericCardExists() {
        zeroOutAllCounts(deck);
        deck.setCount5(1);
        assertThat(deck.isEmpty()).isFalse();
    }

    @Test
    void isEmpty_returnsFalse_whenSpecialCardExists() {
        zeroOutAllCounts(deck);
        deck.setCountFreeze(1);
        assertThat(deck.isEmpty()).isFalse();
    }

    @Test
    void isEmpty_returnsFalse_whenModifierCardExists() {
        zeroOutAllCounts(deck);
        deck.setCountX2(1);
        assertThat(deck.isEmpty()).isFalse();
    }

    @Test
    void decrementCount_reducesAvailableNumericCards() {
        deck.decrementCount(7);
        assertThat(deck.getAvailableCards()).hasSize(INITIAL_SIZE - 1);
    }

    @Test
    void decrementCount_reducesFreezeCount() {
        deck.decrementCount(100);
        assertThat(deck.getCountFreeze()).isEqualTo(2);
        // getAvailableCards adds special cards as 1 entry regardless of count (if > 0)
        assertThat(deck.getAvailableCards()).hasSize(INITIAL_SIZE);
    }

    @Test
    void decrementCount_reducesFlipThreeCount() {
        deck.decrementCount(101);
        assertThat(deck.getCountFlipThree()).isEqualTo(2);
    }

    @Test
    void decrementCount_reducesSecondChanceCount() {
        deck.decrementCount(102);
        assertThat(deck.getCountSecondChance()).isEqualTo(1);
    }

    @Test
    void decrementCount_reducesX2Count() {
        deck.decrementCount(200);
        assertThat(deck.getCountX2()).isZero();
    }

    @Test
    void decrementCount_reducesPlusModifierCounts() {
        deck.decrementCount(201);
        assertThat(deck.getCountPlus2()).isZero();

        deck.reset();
        deck.decrementCount(205);
        assertThat(deck.getCountPlus10()).isZero();
    }

    @Test
    void getAvailableCards_includesAllNonZeroValues() {
        List<Integer> available = deck.getAvailableCards();
        assertThat(available).contains(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        assertThat(available).contains(100, 101, 102);
        assertThat(available).contains(200, 201, 202, 203, 204, 205);
    }

    @Test
    void getAvailableCards_cardCountMatchesInitialComposition() {
        List<Integer> available = deck.getAvailableCards();
        assertThat(available).hasSize(INITIAL_SIZE);

        long numberCards = available.stream().filter(c -> c >= 0 && c <= 12).count();
        long specialCards = available.stream().filter(c -> c >= 100 && c <= 102).count();
        long modifierCards = available.stream().filter(c -> c >= 200).count();

        assertThat(numberCards).isEqualTo(79);
        assertThat(specialCards).isEqualTo(3);
        assertThat(modifierCards).isEqualTo(6);
    }

    @Test
    void getAvailableCards_excludesZeroCountCards() {
        deck.setCount5(0);
        deck.setCountFreeze(0);
        List<Integer> available = deck.getAvailableCards();
        assertThat(available).doesNotContain(5);
        assertThat(available).doesNotContain(100);
    }

    @Test
    void reset_restoresAllCountsAfterDecrements() {
        deck.decrementCount(10);
        deck.decrementCount(100);
        deck.decrementCount(200);
        deck.reset();
        assertThat(deck.getAvailableCards()).hasSize(INITIAL_SIZE);
    }

    private void zeroOutAllCounts(Deck d) {
        d.setCount0(0); d.setCount1(0); d.setCount2(0); d.setCount3(0);
        d.setCount4(0); d.setCount5(0); d.setCount6(0); d.setCount7(0);
        d.setCount8(0); d.setCount9(0); d.setCount10(0); d.setCount11(0);
        d.setCount12(0);
        d.setCountFreeze(0); d.setCountFlipThree(0); d.setCountSecondChance(0);
        d.setCountX2(0); d.setCountPlus2(0); d.setCountPlus4(0);
        d.setCountPlus6(0); d.setCountPlus8(0); d.setCountPlus10(0);
    }
}
