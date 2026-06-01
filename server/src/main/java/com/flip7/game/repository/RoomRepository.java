package com.flip7.game.repository;

import com.flip7.game.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByCode(String code);
    boolean existsByCode(String code);
}
