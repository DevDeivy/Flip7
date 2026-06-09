package com.flip7.game.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class GameControllerIT {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void initMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void createGame_returns200AndGameState() throws Exception {
        mockMvc.perform(post("/api/flip/games")
                        .contentType(MediaType.APPLICATION_JSON)
                .content("{\"players\":[\"Alice\",\"Bob\",\"Charlie\",\"Diana\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLAYING"))
                .andExpect(jsonPath("$.players.length()").value(4));
    }

    @Test
    void createGame_withInvalidPlayers_returns400() throws Exception {
        mockMvc.perform(post("/api/flip/games")
                        .contentType(MediaType.APPLICATION_JSON)
                .content("{\"players\":[\"A\",\"B\",\"C\"]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getGame_withUnknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/flip/games/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

        @Test
        void createAiGame_returnsTwoPlayersAndAiFlag() throws Exception {
        mockMvc.perform(post("/api/flip/games/vs-ai")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"playerName\":\"Erik\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.players.length()").value(2))
            .andExpect(jsonPath("$.players[1].aiControlled").value(true));
        }

        @Test
        void stateStartScoreboardAndWinnerEndpoints_areAvailable() throws Exception {
        String body = mockMvc.perform(post("/api/flip/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"players\":[\"Alice\",\"Bob\",\"Charlie\",\"Diana\"]}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String gameId = body.replaceAll(".*\"gameId\":(\\d+).*", "$1");

        mockMvc.perform(get("/api/flip/games/{gameId}", gameId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gameId").value(Integer.parseInt(gameId)));

        mockMvc.perform(get("/api/flip/games/{gameId}/state", gameId))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/flip/games/{gameId}/start", gameId))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/flip/games/{gameId}/scoreboard", gameId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4));

        mockMvc.perform(get("/api/flip/games/{gameId}/winner", gameId))
            .andExpect(status().isNoContent());
        }
}
