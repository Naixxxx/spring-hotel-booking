package io.staypilot.catalog.application;

import io.staypilot.catalog.api.CatalogDtos.*;
import io.staypilot.catalog.model.Hotel;
import io.staypilot.catalog.model.Room;
import io.staypilot.catalog.persistence.Hotels;
import io.staypilot.catalog.persistence.Rooms;
import io.staypilot.catalog.persistence.RoomSlots;
import io.staypilot.contracts.DomainError;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HotelDirectory {

    private final Hotels hotels;
    private final Rooms rooms;
    private final RoomSlots slots;

    public HotelDirectory(Hotels hotels, Rooms rooms, RoomSlots slots) {
        this.hotels = hotels;
        this.rooms = rooms;
        this.slots = slots;
    }

    @Transactional(readOnly = true)
    public List<HotelView> all() {
        return hotels.findAll().stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public HotelView one(Long id) {
        return view(find(id));
    }

    @Transactional
    public HotelView add(HotelWrite input) {
        return view(hotels.save(new Hotel(input.name(), input.address(), input.city())));
    }

    @Transactional
    public HotelView edit(Long id, HotelPatch input) {
        Hotel hotel = find(id);
        hotel.update(input.name(), input.address(), input.city());
        return view(hotel);
    }

    @Transactional
    public void remove(Long id) {
        Hotel hotel = find(id);
        if (rooms.existsByHotelId(id))
            throw new DomainError(HttpStatus.CONFLICT, "HOTEL_HAS_ROOMS", "Delete rooms before deleting hotel");
        hotels.delete(hotel);
    }

    @Transactional(readOnly = true)
    public HotelStatistics statistics(Long id) {
        find(id);
        List<Room> list = rooms.findByHotelId(id);
        long total = list.stream().mapToLong(Room::getTimesBooked).sum();
        return new HotelStatistics(id, list.size(), list.stream().filter(Room::isAvailable).count(), slots.occupiedCount(id), total);
    }

    Hotel find(Long id) {
        return hotels.findById(id).orElseThrow(() -> new DomainError(HttpStatus.NOT_FOUND, "HOTEL_NOT_FOUND", "Unknown hotel: " + id));
    }

    private HotelView view(Hotel hotel) {
        return new HotelView(hotel.getId(), hotel.getName(), hotel.getAddress(), hotel.getCity(), rooms.findByHotelId(hotel.getId()).size());
    }
}
