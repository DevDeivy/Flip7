package com.flip7.game.model;
import com.flip7.game.RoundPlayerStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "round_players")
@Getter
@Setter
@NoArgsConstructor
public class RoundPlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    private int roundNumber;

    @ElementCollection
    @CollectionTable(name = "round_player_cards", joinColumns = @JoinColumn(name = "round_player_id"))
    @Column(name = "card")
    private List<Integer> currentCards = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "round_player_modifier_cards", joinColumns = @JoinColumn(name = "round_player_id"))
    @Column(name = "modifier_card")
    private List<Integer> modifierCardValues = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private RoundPlayerStatus status = RoundPlayerStatus.ACTIVE;

    private int roundPoints = 0;

    private boolean hasSecondChance = false;

    private boolean hasX2Multiplier = false;

    private int modifierBonus = 0;

    private int pendingFlipThree = 0;
}
