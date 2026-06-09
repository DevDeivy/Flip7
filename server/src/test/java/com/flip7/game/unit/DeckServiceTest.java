package com.flip7.game.unit;

import com.flip7.game.model.Deck;
import com.flip7.game.model.Game;
import com.flip7.game.repository.DeckRepository;
import com.flip7.game.service.DeckService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeckServiceTest {

    @Test
    void createDeck_savesDeckForGame() {
        DeckRepository repository = mock(DeckRepository.class);
        DeckService service = new DeckService(repository);

        when(repository.save(any(Deck.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Game game = new Game();
        game.setId(1L);

        Deck deck = service.createDeck(game);

        assertThat(deck.getGame()).isEqualTo(game);
    }

    @Test
    void isDeckEmpty_throwsWhenDeckNotFound() {
        DeckRepository repository = mock(DeckRepository.class);
        DeckService service = new DeckService(repository);

        Game game = new Game();
        game.setId(1L);

        when(repository.findByGameId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.isDeckEmpty(game))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Mazo no encontrado");
    }

    @Test
    void drawCard_usesOnlyAvailableCardAndDecrementsCount() {
        DeckRepository repository = mock(DeckRepository.class);
        DeckService service = new DeckService(repository);

        Game game = new Game();
        game.setId(1L);

        Deck deck = new Deck();
        deck.setGame(game);
        deck.setCount0(1);
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

        when(repository.findByGameId(1L)).thenReturn(Optional.of(deck));
        when(repository.save(any(Deck.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int card = service.drawCard(game);

        assertThat(card).isEqualTo(0);
        assertThat(deck.getCount0()).isZero();
    }

    @Test
    void drawCard_resetsDeckWhenEmpty() {
        DeckRepository repository = mock(DeckRepository.class);
        DeckService service = new DeckService(repository);

        Game game = new Game();
        game.setId(1L);

        Deck deck = new Deck();
        deck.setGame(game);
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

        when(repository.findByGameId(1L)).thenReturn(Optional.of(deck));
        when(repository.save(any(Deck.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int card = service.drawCard(game);

        assertThat(card).isBetween(0, 205);
        assertThat(deck.isEmpty()).isFalse();
    }
}
