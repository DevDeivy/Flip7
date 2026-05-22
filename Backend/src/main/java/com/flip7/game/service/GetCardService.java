package com.flip7.game.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class GetCardService {

    private int card0 = 1;
    private int card1 = 1;
    private int card2 = 2;
    private int card3 = 3;
    private int card4 = 4;
    private int card5 = 5;
    private int card6 = 6;
    private int card7 = 7;
    private int card8 = 8;
    private int card9 = 9;
    private int card10 = 10;
    private int card11 = 11;
    private int card12 = 12;

    private int points = 0;

    public int generateCard(){
        int numberCard = ThreadLocalRandom.current().nextInt(0, 12);
        points += numberCard;
        return numberCard;
    }

    public int getPoints(){
        return points;
    }
}
