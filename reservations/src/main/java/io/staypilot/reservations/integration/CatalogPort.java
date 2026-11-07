package io.staypilot.reservations.integration;

import io.staypilot.contracts.RoomCard;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CatalogPort {

    List<RoomCard> rankedRooms(LocalDate start, LocalDate end);

    void hold(Long roomId, UUID bookingId, LocalDate start, LocalDate end);

    void release(Long roomId, UUID bookingId, boolean compensation);
}
