package com.flip7.game.repository;

import com.flip7.game.model.RoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, Long> {
    List<RoomParticipant> findByRoomIdOrderByJoinedAtAsc(Long roomId);
    boolean existsByRoomIdAndNameIgnoreCase(Long roomId, String name);
}
