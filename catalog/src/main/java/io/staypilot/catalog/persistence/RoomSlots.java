package io.staypilot.catalog.persistence;

import io.staypilot.catalog.model.RoomSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

public interface RoomSlots extends JpaRepository<RoomSlot, UUID> {

    boolean existsByRoomId(Long roomId);

    @Query("select case when count(s)>0 then true else false end from RoomSlot s " +
        "where s.room.id=:roomId and s.state=io.staypilot.catalog.model.RoomLock.HELD " +
        "and s.startDate < :endDate and s.endDate > :startDate")
    boolean overlaps(@Param("roomId") Long roomId, @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate);

    @Query("select count(s) from RoomSlot s where s.room.id=:roomId and s.state=io.staypilot.catalog.model.RoomLock.HELD")
    long activeCount(@Param("roomId") Long roomId);

    @Query("select count(s) from RoomSlot s where s.room.hotel.id=:hotelId and s.state=io.staypilot.catalog.model.RoomLock.HELD")
    long occupiedCount(@Param("hotelId") Long hotelId);
}
