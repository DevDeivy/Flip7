package com.flip7.game;

import com.flip7.game.DTO.CreateGameDTO;
import com.flip7.game.DTO.CreateRoomDTO;
import com.flip7.game.DTO.FullGameStateDTO;
import com.flip7.game.DTO.JoinRoomDTO;
import com.flip7.game.DTO.RoomParticipantDTO;
import com.flip7.game.DTO.RoomStateDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GameRestIntegrationTest {

        @LocalServerPort
        private int port;

        private String baseUrl(String path) {
                return "http://127.0.0.1:" + port + path;
        }

        private RestClient restClient() {
                return RestClient.builder().baseUrl(baseUrl("")).build();
        }

    @Test
    void createGameEndpoint_returnsPlayingGameWithPlayers() {
        CreateGameDTO request = new CreateGameDTO();
        request.setPlayers(List.of("Alice", "Bob", "Charlie", "Diana"));

        ResponseEntity<FullGameStateDTO> response = restClient()
                .post()
                .uri("/api/flip/games")
                .body(request)
                .retrieve()
                .toEntity(FullGameStateDTO.class);

        FullGameStateDTO body = response.getBody();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo("PLAYING");
        assertThat(body.getPlayers()).hasSize(4);
        assertThat(body.getPlayers())
                .extracting("name")
                .containsExactly("Alice", "Bob", "Charlie", "Diana");
    }

    @Test
    void roomEndpoints_createJoinAndReadRoomStateOverHttp() {
        CreateRoomDTO createRoomDTO = new CreateRoomDTO();
        createRoomDTO.setHostName("HostHttp");

        ResponseEntity<RoomStateDTO> createdRoomResponse = restClient()
                .post()
                .uri("/api/flip/rooms")
                .body(createRoomDTO)
                .retrieve()
                .toEntity(RoomStateDTO.class);
        RoomStateDTO createdRoom = createdRoomResponse.getBody();

        assertThat(createdRoomResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(createdRoom).isNotNull();
        assertThat(createdRoom.getCode()).isNotBlank();

        String code = createdRoom.getCode();

        JoinRoomDTO joinRoomDTO = new JoinRoomDTO();
        joinRoomDTO.setPlayerName("GuestHttp");

        ResponseEntity<RoomStateDTO> joinedRoomResponse = restClient()
                .post()
                .uri("/api/flip/rooms/{code}/join", code)
                .body(joinRoomDTO)
                .retrieve()
                .toEntity(RoomStateDTO.class);
        RoomStateDTO joinedRoom = joinedRoomResponse.getBody();

        assertThat(joinedRoomResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(joinedRoom).isNotNull();
        assertThat(joinedRoom.getCurrentPlayers()).isGreaterThanOrEqualTo(2);

        ResponseEntity<RoomStateDTO> roomStateResponse = restClient()
                .get()
                .uri("/api/flip/rooms/{code}", code)
                .retrieve()
                .toEntity(RoomStateDTO.class);
        RoomStateDTO roomState = roomStateResponse.getBody();

        assertThat(roomStateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(roomState).isNotNull();
        assertThat(roomState.getParticipants())
                .extracting(RoomParticipantDTO::getName)
                .contains("HostHttp", "GuestHttp");
    }
}
