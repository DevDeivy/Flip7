package com.flip7.game;

import com.flip7.game.DTO.CreateGameDTO;
import com.flip7.game.DTO.FullGameStateDTO;
import com.flip7.game.DTO.FullPlayerStateDTO;
import com.flip7.game.model.Game;
import com.flip7.game.repository.DeckRepository;
import com.flip7.game.repository.GameRepository;
import com.flip7.game.service.DeckService;
import com.flip7.game.service.GameService;
import com.flip7.game.service.TurnService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class GameLogicIntegrationTest {

    @Autowired private TurnService turnService;
    @Autowired private GameService gameService;
    @Autowired private GameRepository gameRepository;

    private TestDeckService deckService;
    private Long gameId;

    static class TestDeckService extends DeckService {
        private final List<Integer> cards = new ArrayList<>();
        private int index = 0;

        TestDeckService(DeckRepository deckRepository) {
            super(deckRepository);
        }

        void setCards(List<Integer> cards) {
            this.cards.clear();
            this.cards.addAll(cards);
            this.index = 0;
        }

        @Override
        public int drawCard(Game game) {
            if (index >= cards.size())
                throw new IllegalStateException("No more cards in queue");
            return cards.get(index++);
        }

        @Override
        public boolean isDeckEmpty(Game game) {
            return false;
        }
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        TestDeckService testDeckService(DeckRepository deckRepository) {
            return new TestDeckService(deckRepository);
        }
    }

    void init(String... playerNames) {
        deckService.setCards(List.of());
        CreateGameDTO dto = new CreateGameDTO();
        dto.setPlayers(List.of(playerNames));
        FullGameStateDTO state = gameService.createGame(dto);
        gameId = state.getGameId();
    }

    FullGameStateDTO state() {
        return gameService.getFullState(gameId);
    }

    FullPlayerStateDTO player(int i) {
        return state().getPlayers().get(i);
    }

    // ===============================================================
    // Game creation
    // ===============================================================

    @Test
    void gameCreation_with4Players_succeeds() {
        init("Alice", "Bob", "Charlie", "Diana");
        assertThat(gameId).isPositive();
        FullGameStateDTO s = state();
        assertThat(s.getStatus()).isEqualTo("PLAYING");
        assertThat(s.getPlayers()).hasSize(4);
        assertThat(s.getPlayers()).extracting(FullPlayerStateDTO::getName)
                .containsExactly("Alice", "Bob", "Charlie", "Diana");
        assertThat(s.getCurrentRound()).isEqualTo(1);
        assertThat(s.getCurrentPlayerTurnIndex()).isZero();
    }

    @Test
    void gameCreation_with3Players_throws() {
        CreateGameDTO dto = new CreateGameDTO();
        dto.setPlayers(List.of("A", "B", "C"));
        assertThatThrownBy(() -> gameService.createGame(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("4 y 8");
    }

    // ===============================================================
    // Number card draw
    // ===============================================================

    @Test
    void normalDraw_addsCardAndAdvancesTurn() {
        init("Alice", "Bob", "Charlie", "Diana");
        deckService.setCards(List.of(5));

        String msg = turnService.drawCard(gameId);

        assertThat(msg).contains("Alice").contains("5");
        assertThat(player(0).getRoundCards()).containsExactly(5);
        assertThat(player(0).getRoundPoints()).isEqualTo(5);
        assertThat(player(0).getStatus()).isEqualTo("ACTIVE");
        assertThat(state().getCurrentPlayerTurnIndex()).isEqualTo(1);
    }

    @Test
    void duplicateCard_eliminatesPlayer() {
        init("Alice", "Bob", "Charlie", "Diana");
        deckService.setCards(List.of(4, 2, 2, 7, 4));

        turnService.drawCard(gameId); // Alice 4
        turnService.drawCard(gameId); // Bob   2
        turnService.drawCard(gameId); // Charlie 2
        turnService.drawCard(gameId); // Diana 7

        String msg = turnService.drawCard(gameId); // Alice 4 → duplicate!

        assertThat(msg).contains("eliminado");
        assertThat(player(0).getStatus()).isEqualTo("ELIMINATED");
        assertThat(player(0).getRoundPoints()).isZero();
    }

    @Test
    void sevenUniqueCards_triggersFlip7() {
        init("Alice", "Bob", "Charlie", "Diana");
        deckService.setCards(List.of(1, 9, 9, 9, 2, 3, 4, 5, 6, 7));
        // Alice→1, Bob→9, Charlie→9, Diana→9, Alice→2, Alice→3, Alice→4, Alice→5, Alice→6, Alice→7

        turnService.drawCard(gameId); // Alice 1
        turnService.drawCard(gameId); // Bob   9
        turnService.drawCard(gameId); // Charlie 9
        turnService.drawCard(gameId); // Diana 9

        turnService.drawCard(gameId); // Alice 2
        turnService.stand(gameId);    // Bob stands
        turnService.stand(gameId);    // Charlie stands
        turnService.stand(gameId);    // Diana stands

        turnService.drawCard(gameId); // Alice 3
        turnService.drawCard(gameId); // Alice 4
        turnService.drawCard(gameId); // Alice 5
        turnService.drawCard(gameId); // Alice 6
        String msg = turnService.drawCard(gameId); // Alice 7 → FLIP 7!

        assertThat(msg).contains("FLIP 7");

        // drawCard → handleNumberCard → FLIP 7 → advanceTurn → checkEndOfRound
        // Round advances to 2, all statuses reset to ACTIVE, total points awarded
        assertThat(state().getCurrentRound()).isEqualTo(2);
        assertThat(player(0).getTotalPoints()).isEqualTo(1 + 2 + 3 + 4 + 5 + 6 + 7 + 15);
        assertThat(player(0).getRoundCards()).isEmpty();
        assertThat(player(0).getStatus()).isEqualTo("ACTIVE");
    }

    // ===============================================================
    // Freeze
    // ===============================================================

    @Test
    void freeze_autoStand() {
        init("Alice", "Bob", "Charlie", "Diana");
        deckService.setCards(List.of(100));

        String msg = turnService.drawCard(gameId);

        assertThat(msg).contains("FREEZE");
        assertThat(player(0).getStatus()).isEqualTo("STANDING");
        assertThat(player(0).getRoundPoints()).isZero();
    }

    @Test
    void freeze_withExistingCards_scoresCorrectly() {
        init("Alice", "Bob", "Charlie", "Diana");
        // Alice→5, Bob→7, Charlie→3, Diana→9, Alice→100(FREEZE with 5)
        deckService.setCards(List.of(5, 7, 3, 9, 100));

        turnService.drawCard(gameId); // Alice 5
        turnService.drawCard(gameId); // Bob   7
        turnService.drawCard(gameId); // Charlie 3
        turnService.drawCard(gameId); // Diana 9
        String msg = turnService.drawCard(gameId); // Alice 100 → FREEZE!

        assertThat(msg).contains("FREEZE");
        assertThat(player(0).getStatus()).isEqualTo("STANDING");
        assertThat(player(0).getRoundPoints()).isEqualTo(5);
    }

    // ===============================================================
    // Flip Three
    // ===============================================================

    @Test
    void flipThree_drawsThreeCards() {
        init("Alice", "Bob", "Charlie", "Diana");
        deckService.setCards(List.of(101, 5, 3, 7));

        String msg = turnService.drawCard(gameId);

        assertThat(msg).contains("FLIP THREE");
        assertThat(msg).contains("5 3 7");
        assertThat(player(0).getRoundCards()).containsExactly(5, 3, 7);
        assertThat(player(0).getStatus()).isEqualTo("ACTIVE");
        assertThat(state().getCurrentPlayerTurnIndex()).isEqualTo(1);
    }

    @Test
    void flipThree_withFreezeInside_standsEarly() {
        init("Alice", "Bob", "Charlie", "Diana");
        deckService.setCards(List.of(101, 5, 100));

        String msg = turnService.drawCard(gameId);

        assertThat(msg).contains("FLIP THREE");
        assertThat(msg).contains("FREEZE");
        assertThat(player(0).getRoundCards()).containsExactly(5);
        assertThat(player(0).getStatus()).isEqualTo("STANDING");
        assertThat(player(0).getRoundPoints()).isEqualTo(5);
    }

    @Test
    void flipThree_withDuplicateInside_eliminates() {
        init("Alice", "Bob", "Charlie", "Diana");
        deckService.setCards(List.of(101, 5, 3, 5));

        String msg = turnService.drawCard(gameId);

        assertThat(msg).contains("FLIP THREE");
        assertThat(msg).contains("duplicado");
        assertThat(player(0).getStatus()).isEqualTo("ELIMINATED");
        assertThat(player(0).getRoundPoints()).isZero();
    }

    // ===============================================================
    // Second Chance
    // ===============================================================

    @Test
    void secondChance_grantsProtection() {
        init("Alice", "Bob", "Charlie", "Diana");
        deckService.setCards(List.of(102));

        String msg = turnService.drawCard(gameId);

        assertThat(msg).contains("Segunda Oportunidad");
        assertThat(player(0).isHasSecondChance()).isTrue();
        assertThat(state().getCurrentPlayerTurnIndex()).isEqualTo(1);
    }

    @Test
    void secondChanceAlreadyHas_discardsExtra() {
        init("Alice", "Bob", "Charlie", "Diana");
        // 5 players needed, or use 5 cards: Alice(SC), Bob(9), Charlie(SC), Diana(5), Alice(SC)
        deckService.setCards(List.of(102, 9, 102, 5, 102));

        turnService.drawCard(gameId); // Alice: Second Chance
        turnService.drawCard(gameId); // Bob: 9
        turnService.drawCard(gameId); // Charlie: Second Chance (new)
        turnService.drawCard(gameId); // Diana: 5
        String msg = turnService.drawCard(gameId); // Alice: Second Chance → "ya tenía"

        assertThat(msg).contains("ya tenía");
    }

    @Test
    void secondChance_savesFromDuplicate() {
        init("Alice", "Bob", "Charlie", "Diana");
        deckService.setCards(List.of(102, 9, 9, 9, 5));

        // Round 1: all draw → Alice gets SC, others get 9s
        turnService.drawCard(gameId); // Alice: Second Chance
        turnService.drawCard(gameId); // Bob: 9
        turnService.drawCard(gameId); // Charlie: 9
        turnService.drawCard(gameId); // Diana: 9
        turnService.drawCard(gameId); // Alice: 5

        deckService.setCards(List.of(7, 7, 7, 5));

        turnService.drawCard(gameId); // Bob: 7
        turnService.drawCard(gameId); // Charlie: 7
        turnService.drawCard(gameId); // Diana: 7
        String msg = turnService.drawCard(gameId); // Alice: 5 → saved by SC!

        assertThat(msg).contains("Segunda Oportunidad");
        assertThat(player(0).getStatus()).isEqualTo("ACTIVE");
        assertThat(player(0).isHasSecondChance()).isFalse();
    }

    // ===============================================================
    // Modifier cards
    // ===============================================================

    @Test
    void x2Modifier_doublesScore() {
        init("Alice", "Bob", "Charlie", "Diana");
        deckService.setCards(List.of(200, 5, 9, 9, 9));

        turnService.drawCard(gameId); // Alice: 200 (x2) — turn stays
        turnService.drawCard(gameId); // Alice: 5        — turn advances
        turnService.drawCard(gameId); // Bob: 9
        turnService.drawCard(gameId); // Charlie: 9
        turnService.drawCard(gameId); // Diana: 9
        turnService.stand(gameId);    // Alice stands → x2 applied → 10
        turnService.stand(gameId);    // Bob stands
        turnService.stand(gameId);    // Charlie stands
        turnService.stand(gameId);    // Diana stands → round ends

        assertThat(state().getCurrentRound()).isEqualTo(2);
        assertThat(player(0).getTotalPoints()).isEqualTo(10);
    }

    @Test
    void multipleModifiers_accumulate() {
        init("Alice", "Bob", "Charlie", "Diana");
        deckService.setCards(List.of(201, 204, 7, 9, 9, 9));

        turnService.drawCard(gameId); // Alice: 201 (+2) — turn stays
        turnService.drawCard(gameId); // Alice: 204 (+8) — turn stays
        turnService.drawCard(gameId); // Alice: 7        — turn advances
        turnService.drawCard(gameId); // Bob: 9
        turnService.drawCard(gameId); // Charlie: 9
        turnService.drawCard(gameId); // Diana: 9
        // Alice still ACTIVE — check her modifier cards before she stands
        assertThat(player(0).getModifierCardValues()).containsExactly(201, 204);
        turnService.stand(gameId);    // Alice stands → 7 + 2 + 8 = 17
        turnService.stand(gameId);    // Bob stands
        turnService.stand(gameId);    // Charlie stands
        turnService.stand(gameId);    // Diana stands → round ends

        assertThat(state().getCurrentRound()).isEqualTo(2);
        assertThat(player(0).getTotalPoints()).isEqualTo(7 + 2 + 8);
    }

    // ===============================================================
    // Stand
    // ===============================================================

    @Test
    void stand_afterDraw_standsAndScores() {
        init("Alice", "Bob", "Charlie", "Diana");
        deckService.setCards(List.of(4, 9, 9, 9));

        turnService.drawCard(gameId); // Alice: 4
        turnService.drawCard(gameId); // Bob: 9
        turnService.drawCard(gameId); // Charlie: 9
        turnService.drawCard(gameId); // Diana: 9

        String msg = turnService.stand(gameId); // Alice stands

        assertThat(msg).contains("plantado");
        assertThat(player(0).getStatus()).isEqualTo("STANDING");
        assertThat(player(0).getRoundPoints()).isEqualTo(4);
    }

    @Test
    void stand_withoutDrawing_throws() {
        init("Alice", "Bob", "Charlie", "Diana");
        assertThatThrownBy(() -> turnService.stand(gameId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no ha pedido ninguna carta");
    }

    // ===============================================================
    // End of round & winner
    // ===============================================================

    @Test
    void allPlayersStand_roundAdvances() {
        init("Alice", "Bob", "Charlie", "Diana");
        deckService.setCards(List.of(5, 3, 7, 9));

        turnService.drawCard(gameId); // Alice 5
        turnService.drawCard(gameId); // Bob 3
        turnService.drawCard(gameId); // Charlie 7
        turnService.drawCard(gameId); // Diana 9
        turnService.stand(gameId);    // Alice stands
        turnService.stand(gameId);    // Bob stands
        turnService.stand(gameId);    // Charlie stands
        turnService.stand(gameId);    // Diana stands → all done → round=2

        FullGameStateDTO s = state();
        assertThat(s.getCurrentRound()).isEqualTo(2);
        assertThat(s.getStartingPlayerIndex()).isEqualTo(1);
        assertThat(s.getCurrentPlayerTurnIndex()).isEqualTo(1);
        assertThat(player(0).getTotalPoints()).isEqualTo(5);
        assertThat(player(1).getTotalPoints()).isEqualTo(3);
        assertThat(player(2).getTotalPoints()).isEqualTo(7);
        assertThat(player(3).getTotalPoints()).isEqualTo(9);
        assertThat(player(1).getStatus()).isEqualTo("ACTIVE");
        assertThat(player(0).getRoundCards()).isEmpty();
    }

    @Test
    void allPlayersEliminated_roundAdvances() {
        init("Alice", "Bob", "Charlie", "Diana");
        deckService.setCards(List.of(0, 0, 0, 0, 0, 0, 0, 0));

        turnService.drawCard(gameId); // Alice 0
        turnService.drawCard(gameId); // Bob   0
        turnService.drawCard(gameId); // Charlie 0
        turnService.drawCard(gameId); // Diana 0
        turnService.drawCard(gameId); // Alice 0 → duplicate → ELIMINATED
        turnService.drawCard(gameId); // Bob   0 → duplicate → ELIMINATED
        turnService.drawCard(gameId); // Charlie 0 → duplicate → ELIMINATED
        turnService.drawCard(gameId); // Diana 0 → duplicate → ELIMINATED → all done

        FullGameStateDTO s = state();
        assertThat(s.getCurrentRound()).isEqualTo(2);
        assertThat(s.getPlayers()).allMatch(p -> p.getStatus().equals("ACTIVE"));
        assertThat(s.getPlayers()).allMatch(p -> p.getTotalPoints() == 0);
    }

    @Test
    void playerReaches200_gameFinishes() {
        init("Alice", "Bob", "Charlie", "Diana");

        Game game = gameRepository.findById(gameId).orElseThrow();
        game.getPlayers().get(0).setTotalPoints(200);
        gameRepository.save(game);

        deckService.setCards(List.of(5, 3, 7, 9));

        turnService.drawCard(gameId); // Alice 5
        turnService.drawCard(gameId); // Bob 3
        turnService.drawCard(gameId); // Charlie 7
        turnService.drawCard(gameId); // Diana 9
        turnService.stand(gameId);    // Alice stands
        turnService.stand(gameId);    // Bob stands
        turnService.stand(gameId);    // Charlie stands
        turnService.stand(gameId);    // Diana stands → all done → winner check

        assertThat(state().getStatus()).isEqualTo("FINISHED");
    }

    @Autowired
    public void injectDeckService(DeckService ds) {
        this.deckService = (TestDeckService) ds;
    }
}
