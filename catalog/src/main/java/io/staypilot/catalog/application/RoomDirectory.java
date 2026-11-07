package io.staypilot.catalog.application;

import io.staypilot.catalog.api.CatalogDtos.*;
import io.staypilot.catalog.model.*;
import io.staypilot.catalog.persistence.*;
import io.staypilot.contracts.DomainError;
import io.staypilot.contracts.RoomCard;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class RoomDirectory {

    private final Hotels hotels;
    private final Rooms rooms;
    private final RoomSlots slots;

    public RoomDirectory(Hotels hotels, Rooms rooms, RoomSlots slots) {
        this.hotels = hotels;
        this.rooms = rooms;
        this.slots = slots;
    }

    @Transactional(readOnly = true)
    public List<RoomCard> available(LocalDate start, LocalDate end, boolean ranked) {
        validateRange(start, end);
        List<Room> candidates = ranked ? rooms.findByAvailableTrueOrderByTimesBookedAscIdAsc()
            : rooms.findAll().stream().filter(Room::isAvailable).toList();
        return candidates.stream().filter(r -> !slots.overlaps(r.getId(), start, end)).map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public RoomCard one(Long id) {
        return view(find(id));
    }

    @Transactional
    public RoomCard add(RoomWrite request) {
        Hotel hotel = hotels.findById(request.hotelId()).orElseThrow(() -> new DomainError(HttpStatus.NOT_FOUND, "HOTEL_NOT_FOUND", "Hotel not found"));
        if (rooms.existsByHotelIdAndNumber(hotel.getId(), request.number())) throw duplicateRoom();
        return view(rooms.save(new Room(hotel, request.number(), request.capacity())));
    }

    @Transactional
    public RoomCard edit(Long id, RoomPatch request) {
        Room room = find(id);
        if (request.number() != null && !request.number().equals(room.getNumber())
            && rooms.existsByHotelIdAndNumber(room.getHotel().getId(), request.number())) throw duplicateRoom();
        room.update(request.number(), request.available(), request.capacity());
        return view(room);
    }

    @Transactional
    public void remove(Long id) {
        Room room = find(id);
        if (slots.existsByRoomId(id))
            throw new DomainError(HttpStatus.CONFLICT, "ROOM_HAS_SLOTS", "Room has reservation slot history; disable it instead");
        // A room with historical slots is intentionally preserved for referential integrity.
        if (room.getTimesBooked() > 0)
            throw new DomainError(HttpStatus.CONFLICT, "ROOM_HISTORY", "Room has booking history; disable it instead");
        rooms.delete(room);
    }

    private DomainError duplicateRoom() {
        return new DomainError(HttpStatus.CONFLICT, "DUPLICATE_ROOM", "Room number already exists in hotel");
    }

    private Room find(Long id) {
        return rooms.findById(id).orElseThrow(() -> new DomainError(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "Room not found: " + id));
    }

    private RoomCard view(Room room) {
        return new RoomCard(room.getId(), room.getHotel().getId(), room.getHotel().getName(),
            room.getNumber(), room.isAvailable(), room.getTimesBooked(), room.getCapacity());
    }

    public static void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || !from.isBefore(to) || from.isBefore(LocalDate.now()))
            throw new DomainError(HttpStatus.BAD_REQUEST, "INVALID_DATES", "Use startDate >= today and endDate > startDate");
    }
}
