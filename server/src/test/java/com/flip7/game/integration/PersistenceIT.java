package com.flip7.game.integration;

import com.flip7.game.DTO.CreateGameDTO;
import com.flip7.game.DTO.FullGameStateDTO;
import com.flip7.game.model.Game;
import com.flip7.game.model.RoundPlayer;
import com.flip7.game.model.Room;
import com.flip7.game.repository.DeckRepository;
import com.flip7.game.repository.GameRepository;
import com.flip7.game.repository.RoundPlayerRepository;
import com.flip7.game.repository.RoomRepository;
import com.flip7.game.service.GameService;
import com.flip7.game.service.TurnService;
import com.flip7.game.testutils.DeterministicDeckConfig;
import com.flip7.game.testutils.DeterministicDeckService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(DeterministicDeckConfig.class)
class PersistenceIT {

    @Autowired
    private GameService gameService;

    @Autowired
    private TurnService turnService;

    @Autowired
    private DeterministicDeckService deterministicDeckService;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private RoundPlayerRepository roundPlayerRepository;

    @Autowired
    private RoomRepository roomRepository;

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @org.junit.jupiter.api.BeforeEach
    void initMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void gameRoundAndScoreArePersisted() {
        CreateGameDTO request = new CreateGameDTO();
        request.setPlayers(List.of("Alice", "Bob", "Charlie", "Diana"));
        FullGameStateDTO state = gameService.createGame(request);

        deterministicDeckService.setCards(List.of(5));
        turnService.drawCard(state.getGameId());

        Game game = gameRepository.findById(state.getGameId()).orElseThrow();
        assertThat(deckRepository.findByGameId(state.getGameId())).isPresent();
        List<RoundPlayer> roundPlayers = roundPlayerRepository.findByGameIdAndRoundNumber(state.getGameId(), 1);

        assertThat(roundPlayers).hasSize(1);
        assertThat(roundPlayers.get(0).getCurrentCards()).containsExactly(5);
        assertThat(game.getPlayers()).hasSize(4);
    }

    @Test
    void startRoom_persistsRoomWithGameRelation() throws Exception {
        String roomJson = mockMvc.perform(post("/api/flip/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hostName\":\"Host\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String roomCode = roomJson.replaceAll(".*\\\"code\\\":\\\"([^\\\"]+)\\\".*", "$1");

        for (String name : List.of("P2", "P3", "P4")) {
            mockMvc.perform(post("/api/flip/rooms/{code}/join", roomCode)
                            .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"playerName\":\"" + name + "\"}"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/flip/rooms/{code}/start", roomCode))
                .andExpect(status().isOk());

        Room room = roomRepository.findByCode(roomCode).orElseThrow();
        assertThat(room.getGame()).isNotNull();
        assertThat(gameRepository.findById(room.getGame().getId())).isPresent();

        List<RoundPlayer> roundPlayers = roundPlayerRepository.findByGameIdAndRoundNumber(room.getGame().getId(), 1);
        assertThat(roundPlayers).isEmpty();
    }
}
