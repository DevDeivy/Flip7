package com.flip7.game.integration;

import com.flip7.game.DTO.CreateGameDTO;
import com.flip7.game.DTO.FullGameStateDTO;
import com.flip7.game.model.Game;
import com.flip7.game.repository.GameRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(DeterministicDeckConfig.class)
class WinnerFlowIT {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private GameService gameService;

    @Autowired
    private GameRepository gameRepository;

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
    void winnerEndpoint_returnsNoContentWhenNoWinnerYet() throws Exception {
        mockMvc.perform(get("/api/flip/games/{gameId}/winner", gameId))
                .andExpect(status().isNoContent());
    }

    @Test
    void reachesExactly200_finishesGameAndExposesWinner() throws Exception {
        Game game = gameRepository.findById(gameId).orElseThrow();
        game.getPlayers().get(0).setTotalPoints(195);
        gameRepository.save(game);

        deterministicDeckService.setCards(List.of(5, 3, 4, 6));

        mockMvc.perform(post("/api/flip/games/{gameId}/draw", gameId)).andExpect(status().isOk());
        mockMvc.perform(post("/api/flip/games/{gameId}/draw", gameId)).andExpect(status().isOk());
        mockMvc.perform(post("/api/flip/games/{gameId}/draw", gameId)).andExpect(status().isOk());
        mockMvc.perform(post("/api/flip/games/{gameId}/draw", gameId)).andExpect(status().isOk());

        mockMvc.perform(post("/api/flip/games/{gameId}/stand", gameId)).andExpect(status().isOk());
        mockMvc.perform(post("/api/flip/games/{gameId}/stand", gameId)).andExpect(status().isOk());
        mockMvc.perform(post("/api/flip/games/{gameId}/stand", gameId)).andExpect(status().isOk());

        mockMvc.perform(post("/api/flip/games/{gameId}/stand", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"));

        mockMvc.perform(get("/api/flip/games/{gameId}/winner", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice"))
            .andExpect(jsonPath("$.totalPoints").value(org.hamcrest.Matchers.greaterThanOrEqualTo(200)));
    }
}
