package com.flip7.game.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "decks")
@Getter
@Setter
@NoArgsConstructor
public class Deck {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    private int count0 = 1;
    private int count1 = 1;
    private int count2 = 2;
    private int count3 = 3;
    private int count4 = 4;
    private int count5 = 5;
    private int count6 = 6;
    private int count7 = 7;
    private int count8 = 8;
    private int count9 = 9;
    private int count10 = 10;
    private int count11 = 11;
    private int count12 = 12;

    private int countFreeze = 3;
    private int countFlipThree = 3;
    private int countSecondChance = 2;

    private int countX2 = 1;
    private int countPlus2 = 1;
    private int countPlus4 = 1;
    private int countPlus6 = 1;
    private int countPlus8 = 1;
    private int countPlus10 = 1;

    public void reset() {
        count0 = 1; count1 = 1; count2 = 2; count3 = 3;
        count4 = 4; count5 = 5; count6 = 6; count7 = 7;
        count8 = 8; count9 = 9; count10 = 10; count11 = 11;
        count12 = 12;
        countFreeze = 3;
        countFlipThree = 3;
        countSecondChance = 2;
        countX2 = 1;
        countPlus2 = 1;
        countPlus4 = 1;
        countPlus6 = 1;
        countPlus8 = 1;
        countPlus10 = 1;
    }

    public boolean isEmpty() {
        return count0 + count1 + count2 + count3 + count4 +
                count5 + count6 + count7 + count8 + count9 +
                count10 + count11 + count12 +
                countFreeze + countFlipThree + countSecondChance +
                countX2 + countPlus2 + countPlus4 + countPlus6 + countPlus8 + countPlus10 == 0;
    }

    public List<Integer> getAvailableCards() {
        List<Integer> available = new ArrayList<>();
        if (count0 > 0) available.add(0);
        if (count1 > 0) available.add(1);
        if (count2 > 0) { for (int i = 0; i < count2; i++) available.add(2); }
        if (count3 > 0) { for (int i = 0; i < count3; i++) available.add(3); }
        if (count4 > 0) { for (int i = 0; i < count4; i++) available.add(4); }
        if (count5 > 0) { for (int i = 0; i < count5; i++) available.add(5); }
        if (count6 > 0) { for (int i = 0; i < count6; i++) available.add(6); }
        if (count7 > 0) { for (int i = 0; i < count7; i++) available.add(7); }
        if (count8 > 0) { for (int i = 0; i < count8; i++) available.add(8); }
        if (count9 > 0) { for (int i = 0; i < count9; i++) available.add(9); }
        if (count10 > 0) { for (int i = 0; i < count10; i++) available.add(10); }
        if (count11 > 0) { for (int i = 0; i < count11; i++) available.add(11); }
        if (count12 > 0) { for (int i = 0; i < count12; i++) available.add(12); }
        if (countFreeze > 0) available.add(100);
        if (countFlipThree > 0) available.add(101);
        if (countSecondChance > 0) available.add(102);
        if (countX2 > 0) available.add(200);
        if (countPlus2 > 0) available.add(201);
        if (countPlus4 > 0) available.add(202);
        if (countPlus6 > 0) available.add(203);
        if (countPlus8 > 0) available.add(204);
        if (countPlus10 > 0) available.add(205);
        return available;
    }

    public void decrementCount(int card) {
        switch (card) {
            case 0 -> count0--;
            case 1 -> count1--;
            case 2 -> count2--;
            case 3 -> count3--;
            case 4 -> count4--;
            case 5 -> count5--;
            case 6 -> count6--;
            case 7 -> count7--;
            case 8 -> count8--;
            case 9 -> count9--;
            case 10 -> count10--;
            case 11 -> count11--;
            case 12 -> count12--;
            case 100 -> countFreeze--;
            case 101 -> countFlipThree--;
            case 102 -> countSecondChance--;
            case 200 -> countX2--;
            case 201 -> countPlus2--;
            case 202 -> countPlus4--;
            case 203 -> countPlus6--;
            case 204 -> countPlus8--;
            case 205 -> countPlus10--;
        }
    }
}
