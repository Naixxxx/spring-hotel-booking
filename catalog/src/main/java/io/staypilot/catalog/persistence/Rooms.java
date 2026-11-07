package io.staypilot.catalog.persistence;

import io.staypilot.catalog.model.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface Rooms extends JpaRepository<Room, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id=:id")
    Optional<Room> lockById(@Param("id") Long id);

    List<Room> findByAvailableTrueOrderByTimesBookedAscIdAsc();

    List<Room> findByHotelId(Long hotelId);

    boolean existsByHotelId(Long hotelId);

    boolean existsByHotelIdAndNumber(Long hotelId, String number);
}
