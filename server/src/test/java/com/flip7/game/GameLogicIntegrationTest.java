package com.flip7.game;

import com.flip7.game.DTO.CreateGameDTO;
import com.flip7.game.DTO.FullGameStateDTO;
import com.flip7.game.DTO.FullPlayerStateDTO;
import com.flip7.game.model.Deck;
import com.flip7.game.model.Game;
import com.flip7.game.repository.DeckRepository;
import com.flip7.game.repository.GameRepository;
import com.flip7.game.service.GameService;
import com.flip7.game.service.TurnService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class GameLogicIntegrationTest {

    @Autowired private TurnService turnService;
    @Autowired private GameService gameService;
    @Autowired private GameRepository gameRepository;
    @Autowired private DeckRepository deckRepository;

    private Long gameId;

    void init(String... playerNames) {
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

    private void configureDeck(int... cards) {
        Deck deck = deckRepository.findByGameId(gameId).orElseThrow();
        zeroDeck(deck);
        Map<Integer, Integer> counts = new HashMap<>();
        for (int card : cards) {
            counts.merge(card, 1, Integer::sum);
        }
        counts.forEach((card, count) -> setCardCount(deck, card, count));
        deckRepository.saveAndFlush(deck);
    }

    private void zeroDeck(Deck deck) {
        for (int i = 0; i <= 12; i++) setCardCount(deck, i, 0);
        setCardCount(deck, 100, 0);
        setCardCount(deck, 101, 0);
        setCardCount(deck, 102, 0);
        setCardCount(deck, 200, 0);
        setCardCount(deck, 201, 0);
        setCardCount(deck, 202, 0);
        setCardCount(deck, 203, 0);
        setCardCount(deck, 204, 0);
        setCardCount(deck, 205, 0);
    }

    private void setCardCount(Deck deck, int card, int count) {
        switch (card) {
            case 0 -> deck.setCount0(count);
            case 1 -> deck.setCount1(count);
            case 2 -> deck.setCount2(count);
            case 3 -> deck.setCount3(count);
            case 4 -> deck.setCount4(count);
            case 5 -> deck.setCount5(count);
            case 6 -> deck.setCount6(count);
            case 7 -> deck.setCount7(count);
            case 8 -> deck.setCount8(count);
            case 9 -> deck.setCount9(count);
            case 10 -> deck.setCount10(count);
            case 11 -> deck.setCount11(count);
            case 12 -> deck.setCount12(count);
            case 100 -> deck.setCountFreeze(count);
            case 101 -> deck.setCountFlipThree(count);
            case 102 -> deck.setCountSecondChance(count);
            case 200 -> deck.setCountX2(count);
            case 201 -> deck.setCountPlus2(count);
            case 202 -> deck.setCountPlus4(count);
            case 203 -> deck.setCountPlus6(count);
            case 204 -> deck.setCountPlus8(count);
            case 205 -> deck.setCountPlus10(count);
        }
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
        configureDeck(5);

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

        configureDeck(4); turnService.drawCard(gameId); // Alice 4
        configureDeck(2); turnService.drawCard(gameId); // Bob   2
        configureDeck(2); turnService.drawCard(gameId); // Charlie 2
        configureDeck(7); turnService.drawCard(gameId); // Diana 7
        configureDeck(4); turnService.drawCard(gameId); // Alice 4 -> duplicate!

        assertThat(player(0).getStatus()).isEqualTo("ELIMINATED");
        assertThat(player(0).getRoundPoints()).isZero();
    }

    @Test
    void sevenUniqueCards_triggersFlip7() {
        init("Alice", "Bob", "Charlie", "Diana");

        configureDeck(1); turnService.drawCard(gameId); // Alice 1
        configureDeck(9); turnService.drawCard(gameId); // Bob   9
        configureDeck(9); turnService.drawCard(gameId); // Charlie 9
        configureDeck(9); turnService.drawCard(gameId); // Diana 9

        configureDeck(2); turnService.drawCard(gameId); // Alice 2

        turnService.stand(gameId);    // Bob stands
        turnService.stand(gameId);    // Charlie stands
        turnService.stand(gameId);    // Diana stands

        configureDeck(3); turnService.drawCard(gameId); // Alice 3
        configureDeck(4); turnService.drawCard(gameId); // Alice 4
        configureDeck(5); turnService.drawCard(gameId); // Alice 5
        configureDeck(6); turnService.drawCard(gameId); // Alice 6
        String msg = turnService.drawCard(gameId); // Alice 7 -> FLIP 7!

        assertThat(msg).contains("FLIP 7");
        assertThat(state().getCurrentRound()).isEqualTo(2);
        assertThat(player(0).getRoundCards()).isEmpty();
        assertThat(player(0).getStatus()).isEqualTo("ACTIVE");
        assertThat(player(0).getTotalPoints()).isPositive();
    }

    // ===============================================================
    // Freeze
    // ===============================================================

    @Test
    void freeze_autoStand() {
        init("Alice", "Bob", "Charlie", "Diana");
        configureDeck(100);

        String msg = turnService.drawCard(gameId);

        assertThat(msg).contains("FREEZE");
        assertThat(player(0).getStatus()).isEqualTo("STANDING");
        assertThat(player(0).getRoundPoints()).isZero();
    }

    @Test
    void freeze_withExistingCards_scoresCorrectly() {
        init("Alice", "Bob", "Charlie", "Diana");

        configureDeck(5);   turnService.drawCard(gameId); // Alice 5
        configureDeck(7);   turnService.drawCard(gameId); // Bob   7
        configureDeck(3);   turnService.drawCard(gameId); // Charlie 3
        configureDeck(9);   turnService.drawCard(gameId); // Diana 9
        configureDeck(100); turnService.drawCard(gameId); // Alice 100 -> FREEZE!

        assertThat(player(0).getStatus()).isEqualTo("STANDING");
        assertThat(player(0).getRoundPoints()).isEqualTo(5);
    }

    // ===============================================================
    // Flip Three
    // ===============================================================

    @Test
    void flipThree_drawsThreeCards() {
        init("Alice", "Bob", "Charlie", "Diana");
        configureDeck(101);

        String msg = turnService.drawCard(gameId);

        assertThat(msg).contains("FLIP THREE");
    }

    // ===============================================================
    // Second Chance
    // ===============================================================

    @Test
    void secondChance_grantsProtection() {
        init("Alice", "Bob", "Charlie", "Diana");
        configureDeck(102);

        String msg = turnService.drawCard(gameId);

        assertThat(msg).contains("Segunda Oportunidad");
        assertThat(player(0).isHasSecondChance()).isTrue();
        assertThat(state().getCurrentPlayerTurnIndex()).isEqualTo(1);
    }

    @Test
    void secondChanceAlreadyHas_discardsExtra() {
        init("Alice", "Bob", "Charlie", "Diana");

        configureDeck(102); turnService.drawCard(gameId); // Alice: Second Chance
        configureDeck(9);   turnService.drawCard(gameId); // Bob: 9
        configureDeck(102); turnService.drawCard(gameId); // Charlie: Second Chance
        configureDeck(5);   turnService.drawCard(gameId); // Diana: 5
        configureDeck(102); turnService.drawCard(gameId); // Alice: Second Chance -> "ya tenía"

        assertThat(player(0).isHasSecondChance()).isTrue();
    }

    @Test
    void secondChance_savesFromDuplicate() {
        init("Alice", "Bob", "Charlie", "Diana");

        configureDeck(102); turnService.drawCard(gameId); // Alice: Second Chance
        configureDeck(9);   turnService.drawCard(gameId); // Bob: 9
        configureDeck(9);   turnService.drawCard(gameId); // Charlie: 9
        configureDeck(9);   turnService.drawCard(gameId); // Diana: 9
        configureDeck(5);   turnService.drawCard(gameId); // Alice: 5

        configureDeck(7); turnService.drawCard(gameId); // Bob: 7
        configureDeck(7); turnService.drawCard(gameId); // Charlie: 7
        configureDeck(7); turnService.drawCard(gameId); // Diana: 7
        configureDeck(5); turnService.drawCard(gameId); // Alice: 5 -> saved by SC!

        assertThat(player(0).getStatus()).isEqualTo("ACTIVE");
        assertThat(player(0).isHasSecondChance()).isFalse();
    }

    // ===============================================================
    // Modifier cards
    // ===============================================================

    @Test
    void x2Modifier_doublesScore() {
        init("Alice", "Bob", "Charlie", "Diana");

        configureDeck(200); turnService.drawCard(gameId); // Alice: x2
        configureDeck(5);   turnService.drawCard(gameId); // Alice: 5
        configureDeck(9);   turnService.drawCard(gameId); // Bob: 9
        configureDeck(9);   turnService.drawCard(gameId); // Charlie: 9
        configureDeck(9);   turnService.drawCard(gameId); // Diana: 9

        turnService.stand(gameId);    // Alice stands -> x2 applied -> 10
        turnService.stand(gameId);    // Bob stands
        turnService.stand(gameId);    // Charlie stands
        turnService.stand(gameId);    // Diana stands -> round ends

        assertThat(state().getCurrentRound()).isEqualTo(2);
        assertThat(player(0).getTotalPoints()).isEqualTo(10);
    }

    @Test
    void multipleModifiers_accumulate() {
        init("Alice", "Bob", "Charlie", "Diana");

        configureDeck(201); turnService.drawCard(gameId); // Alice: +2
        configureDeck(204); turnService.drawCard(gameId); // Alice: +8
        configureDeck(7);   turnService.drawCard(gameId); // Alice: 7
        configureDeck(9);   turnService.drawCard(gameId); // Bob: 9
        configureDeck(9);   turnService.drawCard(gameId); // Charlie: 9
        configureDeck(9);   turnService.drawCard(gameId); // Diana: 9

        assertThat(player(0).getModifierCardValues()).containsExactly(201, 204);
        turnService.stand(gameId);    // Alice stands -> 7 + 2 + 8 = 17
        turnService.stand(gameId);    // Bob stands
        turnService.stand(gameId);    // Charlie stands
        turnService.stand(gameId);    // Diana stands -> round ends

        assertThat(state().getCurrentRound()).isEqualTo(2);
        assertThat(player(0).getTotalPoints()).isEqualTo(7 + 2 + 8);
    }

    // ===============================================================
    // Stand
    // ===============================================================

    @Test
    void stand_afterDraw_standsAndScores() {
        init("Alice", "Bob", "Charlie", "Diana");

        configureDeck(4); turnService.drawCard(gameId); // Alice: 4
        configureDeck(9); turnService.drawCard(gameId); // Bob: 9
        configureDeck(9); turnService.drawCard(gameId); // Charlie: 9
        configureDeck(9); turnService.drawCard(gameId); // Diana: 9

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

        configureDeck(5); turnService.drawCard(gameId); // Alice 5
        configureDeck(3); turnService.drawCard(gameId); // Bob 3
        configureDeck(7); turnService.drawCard(gameId); // Charlie 7
        configureDeck(9); turnService.drawCard(gameId); // Diana 9

        turnService.stand(gameId);    // Alice stands
        turnService.stand(gameId);    // Bob stands
        turnService.stand(gameId);    // Charlie stands
        turnService.stand(gameId);    // Diana stands -> all done -> round=2

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

        configureDeck(0); turnService.drawCard(gameId); // Alice 0
        configureDeck(0); turnService.drawCard(gameId); // Bob   0
        configureDeck(0); turnService.drawCard(gameId); // Charlie 0
        configureDeck(0); turnService.drawCard(gameId); // Diana 0
        configureDeck(0); turnService.drawCard(gameId); // Alice 0 -> duplicate -> ELIMINATED
        configureDeck(0); turnService.drawCard(gameId); // Bob   0 -> duplicate -> ELIMINATED
        configureDeck(0); turnService.drawCard(gameId); // Charlie 0 -> duplicate -> ELIMINATED
        configureDeck(0); turnService.drawCard(gameId); // Diana 0 -> duplicate -> ELIMINATED

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

        configureDeck(5); turnService.drawCard(gameId); // Alice 5
        configureDeck(3); turnService.drawCard(gameId); // Bob 3
        configureDeck(7); turnService.drawCard(gameId); // Charlie 7
        configureDeck(9); turnService.drawCard(gameId); // Diana 9
        turnService.stand(gameId);    // Alice stands
        turnService.stand(gameId);    // Bob stands
        turnService.stand(gameId);    // Charlie stands
        turnService.stand(gameId);    // Diana stands -> all done -> winner check

        assertThat(state().getStatus()).isEqualTo("FINISHED");
    }
}
