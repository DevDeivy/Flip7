package com.flip7.game.integration;

import com.flip7.game.DTO.CreateGameDTO;
import com.flip7.game.DTO.FullGameStateDTO;
import com.flip7.game.service.GameService;
import com.flip7.game.testutils.DeterministicDeckConfig;
import com.flip7.game.testutils.DeterministicDeckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(DeterministicDeckConfig.class)
class TurnFlowIT {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private GameService gameService;

    @Autowired
    private DeterministicDeckService deterministicDeckService;

    private Long gameId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        CreateGameDTO request = new CreateGameDTO();
        request.setPlayers(List.of("Alice", "Bob", "Charlie", "Diana"));
        FullGameStateDTO state = gameService.createGame(request);
        gameId = state.getGameId();
    }

    @Test
    void drawCard_endpointUpdatesState() throws Exception {
        deterministicDeckService.setCards(List.of(5));

        mockMvc.perform(post("/api/flip/games/{gameId}/draw", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastMessage").value(org.hamcrest.Matchers.containsString("5")))
                .andExpect(jsonPath("$.currentPlayerTurnIndex").value(1));
    }

    @Test
    void stand_withoutAnyCard_returns400() throws Exception {
        mockMvc.perform(post("/api/flip/games/{gameId}/stand", gameId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateCard_marksPlayerAsEliminated() throws Exception {
        deterministicDeckService.setCards(List.of(5, 9, 8, 7, 5));

        mockMvc.perform(post("/api/flip/games/{gameId}/draw", gameId)).andExpect(status().isOk());
        mockMvc.perform(post("/api/flip/games/{gameId}/draw", gameId)).andExpect(status().isOk());
        mockMvc.perform(post("/api/flip/games/{gameId}/draw", gameId)).andExpect(status().isOk());
        mockMvc.perform(post("/api/flip/games/{gameId}/draw", gameId)).andExpect(status().isOk());

        mockMvc.perform(post("/api/flip/games/{gameId}/draw", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicateAlert.cardValue").value(5))
                .andExpect(jsonPath("$.duplicateAlert.playerName").value("Alice"));
    }
}
