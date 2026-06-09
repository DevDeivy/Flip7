package com.flip7.game.unit;

import com.flip7.game.DTO.FullGameStateDTO;
import com.flip7.game.DTO.RoomStateDTO;
import com.flip7.game.RoomStatus;
import com.flip7.game.model.Game;
import com.flip7.game.model.Room;
import com.flip7.game.model.RoomParticipant;
import com.flip7.game.repository.RoomParticipantRepository;
import com.flip7.game.repository.RoomRepository;
import com.flip7.game.service.GameService;
import com.flip7.game.service.RoomService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoomServiceTest {

    @Test
    void createRoom_createsHostAndReturnsRoomState() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomParticipantRepository participantRepository = mock(RoomParticipantRepository.class);
        GameService gameService = mock(GameService.class);
        RoomService roomService = new RoomService(roomRepository, participantRepository, gameService);

        when(roomRepository.existsByCode(any(String.class))).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> {
            Room room = invocation.getArgument(0);
            room.setId(10L);
            if (room.getCreatedAt() == null) {
                room.setCreatedAt(Instant.now());
            }
            return room;
        });
        when(participantRepository.save(any(RoomParticipant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roomRepository.findByCode(any(String.class))).thenAnswer(invocation -> {
            Room room = new Room();
            room.setId(10L);
            room.setCode(invocation.getArgument(0));
            room.setStatus(RoomStatus.WAITING);
            room.setHostName("Ana");
            return Optional.of(room);
        });
        when(participantRepository.findByRoomIdOrderByJoinedAtAsc(10L)).thenReturn(List.of(participant("Ana")));

        RoomStateDTO state = roomService.createRoom("  Ana  ");

        assertThat(state.getCode()).hasSize(6);
        assertThat(state.getStatus()).isEqualTo("WAITING");
        assertThat(state.getHostName()).isEqualTo("Ana");
        assertThat(state.getCurrentPlayers()).isEqualTo(1);
        assertThat(state.getParticipants()).hasSize(1);
        assertThat(state.getParticipants().get(0).getName()).isEqualTo("Ana");
    }

    @Test
    void joinRoom_addsParticipantAndReturnsNormalizedState() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomParticipantRepository participantRepository = mock(RoomParticipantRepository.class);
        GameService gameService = mock(GameService.class);
        RoomService roomService = new RoomService(roomRepository, participantRepository, gameService);

        Room room = new Room();
        room.setId(1L);
        room.setCode("ABC123");
        room.setStatus(RoomStatus.WAITING);
        room.setHostName("Host");

        when(roomRepository.findByCode("ABC123")).thenReturn(Optional.of(room));
        when(participantRepository.findByRoomIdOrderByJoinedAtAsc(1L))
                .thenReturn(List.of(participant("Host")))
                .thenReturn(List.of(participant("Host"), participant("Alice")));
        when(participantRepository.existsByRoomIdAndNameIgnoreCase(1L, "Alice")).thenReturn(false);
        when(participantRepository.save(any(RoomParticipant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoomStateDTO state = roomService.joinRoom("abc123", "  Alice  ");

        assertThat(state.getCode()).isEqualTo("ABC123");
        assertThat(state.getCurrentPlayers()).isEqualTo(2);
        assertThat(state.getParticipants()).extracting("name").containsExactly("Host", "Alice");
    }

    @Test
    void getRoomState_mapsExistingGameId() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomParticipantRepository participantRepository = mock(RoomParticipantRepository.class);
        GameService gameService = mock(GameService.class);
        RoomService roomService = new RoomService(roomRepository, participantRepository, gameService);

        Room room = new Room();
        room.setId(8L);
        room.setCode("ROOM88");
        room.setStatus(RoomStatus.STARTED);
        room.setHostName("Host");
        Game game = new Game();
        game.setId(99L);
        room.setGame(game);

        when(roomRepository.findByCode("ROOM88")).thenReturn(Optional.of(room));
        when(participantRepository.findByRoomIdOrderByJoinedAtAsc(8L))
                .thenReturn(List.of(participant("Host"), participant("Bob")));

        RoomStateDTO state = roomService.getRoomState("room88");

        assertThat(state.getGameId()).isEqualTo(99L);
        assertThat(state.getStatus()).isEqualTo("STARTED");
        assertThat(state.getParticipants()).hasSize(2);
    }

    @Test
    void startRoom_marksRoomStartedAndReturnsGameState() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomParticipantRepository participantRepository = mock(RoomParticipantRepository.class);
        GameService gameService = mock(GameService.class);
        RoomService roomService = new RoomService(roomRepository, participantRepository, gameService);

        Room room = new Room();
        room.setId(7L);
        room.setCode("ABCD12");
        room.setStatus(RoomStatus.WAITING);

        List<RoomParticipant> participants = List.of(
                participant("A"), participant("B"), participant("C"), participant("D")
        );

        FullGameStateDTO created = new FullGameStateDTO();
        created.setGameId(77L);
        Game game = new Game();
        game.setId(77L);

        when(roomRepository.findByCode("ABCD12")).thenReturn(Optional.of(room));
        when(participantRepository.findByRoomIdOrderByJoinedAtAsc(7L)).thenReturn(participants);
        when(gameService.createGame(List.of("A", "B", "C", "D"))).thenReturn(created);
        when(gameService.findGameById(77L)).thenReturn(game);
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FullGameStateDTO state = roomService.startRoom("abcd12");

        assertThat(state.getGameId()).isEqualTo(77L);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.STARTED);
        assertThat(room.getGame()).isNotNull();
        assertThat(room.getGame().getId()).isEqualTo(77L);
    }

    @Test
    void getRoomState_throwsWhenRoomDoesNotExist() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomParticipantRepository participantRepository = mock(RoomParticipantRepository.class);
        GameService gameService = mock(GameService.class);
        RoomService roomService = new RoomService(roomRepository, participantRepository, gameService);

        when(roomRepository.findByCode("ABC123")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.getRoomState("abc123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sala no encontrada");
    }

    @Test
    void createRoom_throwsWhenHostNameIsBlank() {
        RoomService roomService = new RoomService(mock(RoomRepository.class), mock(RoomParticipantRepository.class), mock(GameService.class));

        assertThatThrownBy(() -> roomService.createRoom("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre no puede estar vacío");
    }

    @Test
    void joinRoom_throwsWhenRoomIsNotWaiting() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomParticipantRepository participantRepository = mock(RoomParticipantRepository.class);
        GameService gameService = mock(GameService.class);
        RoomService roomService = new RoomService(roomRepository, participantRepository, gameService);

        Room room = new Room();
        room.setId(1L);
        room.setCode("ABC123");
        room.setStatus(RoomStatus.STARTED);

        when(roomRepository.findByCode("ABC123")).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> roomService.joinRoom("abc123", "Alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya no está disponible");
    }

    @Test
    void joinRoom_throwsWhenRoomIsFull() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomParticipantRepository participantRepository = mock(RoomParticipantRepository.class);
        GameService gameService = mock(GameService.class);
        RoomService roomService = new RoomService(roomRepository, participantRepository, gameService);

        Room room = new Room();
        room.setId(1L);
        room.setCode("ABC123");
        room.setStatus(RoomStatus.WAITING);

        List<RoomParticipant> eightPlayers = java.util.stream.IntStream.range(0, 8)
                .mapToObj(index -> {
                    RoomParticipant participant = new RoomParticipant();
                    participant.setName("P" + index);
                    return participant;
                })
                .toList();

        when(roomRepository.findByCode("ABC123")).thenReturn(Optional.of(room));
        when(participantRepository.findByRoomIdOrderByJoinedAtAsc(1L)).thenReturn(eightPlayers);

        assertThatThrownBy(() -> roomService.joinRoom("abc123", "Alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("máximo de 8 jugadores");
    }

    @Test
    void joinRoom_throwsWhenNameAlreadyExists() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomParticipantRepository participantRepository = mock(RoomParticipantRepository.class);
        GameService gameService = mock(GameService.class);
        RoomService roomService = new RoomService(roomRepository, participantRepository, gameService);

        Room room = new Room();
        room.setId(1L);
        room.setCode("ABC123");
        room.setStatus(RoomStatus.WAITING);

        when(roomRepository.findByCode("ABC123")).thenReturn(Optional.of(room));
        when(participantRepository.findByRoomIdOrderByJoinedAtAsc(1L)).thenReturn(List.of(new RoomParticipant()));
        when(participantRepository.existsByRoomIdAndNameIgnoreCase(1L, "Alice")).thenReturn(true);

        assertThatThrownBy(() -> roomService.joinRoom("abc123", "Alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe un jugador");
    }

    @Test
    void startRoom_throwsWhenNotEnoughPlayers() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomParticipantRepository participantRepository = mock(RoomParticipantRepository.class);
        GameService gameService = mock(GameService.class);
        RoomService roomService = new RoomService(roomRepository, participantRepository, gameService);

        Room room = new Room();
        room.setId(1L);
        room.setCode("ABC123");
        room.setStatus(RoomStatus.WAITING);

        when(roomRepository.findByCode("ABC123")).thenReturn(Optional.of(room));
        when(participantRepository.findByRoomIdOrderByJoinedAtAsc(1L))
                .thenReturn(List.of(new RoomParticipant(), new RoomParticipant(), new RoomParticipant()));

        assertThatThrownBy(() -> roomService.startRoom("abc123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos 4 jugadores");
    }

    private RoomParticipant participant(String name) {
        RoomParticipant participant = new RoomParticipant();
        participant.setName(name);
        return participant;
    }
}
