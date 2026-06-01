package com.flip7.game.service;

import com.flip7.game.DTO.FullGameStateDTO;
import com.flip7.game.DTO.RoomParticipantDTO;
import com.flip7.game.DTO.RoomStateDTO;
import com.flip7.game.RoomStatus;
import com.flip7.game.model.Room;
import com.flip7.game.model.RoomParticipant;
import com.flip7.game.repository.RoomParticipantRepository;
import com.flip7.game.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class RoomService {
    private static final int MIN_PLAYERS = 4;
    private static final int MAX_PLAYERS = 8;

    private final RoomRepository roomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final GameService gameService;

    @Transactional
    public RoomStateDTO createRoom(String hostName) {
        String normalizedHost = normalizeName(hostName);

        Room room = new Room();
        room.setCode(generateRoomCode());
        room.setStatus(RoomStatus.WAITING);
        room.setHostName(normalizedHost);
        room.setCreatedAt(Instant.now());
        roomRepository.save(room);

        RoomParticipant host = new RoomParticipant();
        host.setName(normalizedHost);
        host.setRoom(room);
        roomParticipantRepository.save(host);

        return getRoomState(room.getCode());
    }

    @Transactional
    public RoomStateDTO joinRoom(String roomCode, String playerName) {
        Room room = getWaitingRoom(roomCode);
        String normalizedName = normalizeName(playerName);

        int currentPlayers = roomParticipantRepository.findByRoomIdOrderByJoinedAtAsc(room.getId()).size();
        if (currentPlayers >= MAX_PLAYERS) {
            throw new IllegalArgumentException("La sala ya alcanzó el máximo de 8 jugadores");
        }

        if (roomParticipantRepository.existsByRoomIdAndNameIgnoreCase(room.getId(), normalizedName)) {
            throw new IllegalArgumentException("Ya existe un jugador con ese nombre en la sala");
        }

        RoomParticipant participant = new RoomParticipant();
        participant.setName(normalizedName);
        participant.setRoom(room);
        roomParticipantRepository.save(participant);

        return getRoomState(roomCode);
    }

    @Transactional(readOnly = true)
    public RoomStateDTO getRoomState(String roomCode) {
        Room room = roomRepository.findByCode(roomCode.toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Sala no encontrada"));

        List<RoomParticipant> participants = roomParticipantRepository.findByRoomIdOrderByJoinedAtAsc(room.getId());

        RoomStateDTO dto = new RoomStateDTO();
        dto.setRoomId(room.getId());
        dto.setCode(room.getCode());
        dto.setStatus(room.getStatus().name());
        dto.setHostName(room.getHostName());
        dto.setCurrentPlayers(participants.size());
        dto.setMinimumPlayersToStart(MIN_PLAYERS);
        dto.setGameId(room.getGame() != null ? room.getGame().getId() : null);
        dto.setParticipants(participants.stream().map(participant -> {
            RoomParticipantDTO p = new RoomParticipantDTO();
            p.setId(participant.getId());
            p.setName(participant.getName());
            return p;
        }).toList());

        return dto;
    }

    @Transactional
    public FullGameStateDTO startRoom(String roomCode) {
        Room room = getWaitingRoom(roomCode);
        List<RoomParticipant> participants = roomParticipantRepository.findByRoomIdOrderByJoinedAtAsc(room.getId());

        if (participants.size() < MIN_PLAYERS) {
            throw new IllegalArgumentException("Se necesitan al menos 4 jugadores para iniciar la sala");
        }

        FullGameStateDTO state = gameService.createGame(
                participants.stream().map(RoomParticipant::getName).toList()
        );

        room.setStatus(RoomStatus.STARTED);
        room.setGame(gameService.findGameById(state.getGameId()));
        roomRepository.save(room);

        return state;
    }

    private Room getWaitingRoom(String roomCode) {
        Room room = roomRepository.findByCode(roomCode.toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Sala no encontrada"));

        if (room.getStatus() != RoomStatus.WAITING) {
            throw new IllegalArgumentException("La sala ya no está disponible para unirse");
        }

        return room;
    }

    private String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        return normalized;
    }

    private String generateRoomCode() {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        for (int attempts = 0; attempts < 20; attempts += 1) {
            StringBuilder code = new StringBuilder();
            for (int index = 0; index < 6; index += 1) {
                code.append(alphabet.charAt(ThreadLocalRandom.current().nextInt(alphabet.length())));
            }

            String value = code.toString();
            if (!roomRepository.existsByCode(value)) {
                return value;
            }
        }

        throw new IllegalStateException("No se pudo generar un código de sala único");
    }
}
