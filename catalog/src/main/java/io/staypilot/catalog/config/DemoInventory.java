package io.staypilot.catalog.config;

import io.staypilot.catalog.model.Hotel;
import io.staypilot.catalog.model.Room;
import io.staypilot.catalog.persistence.Hotels;
import io.staypilot.catalog.persistence.Rooms;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoInventory {

    @Bean
    ApplicationRunner roomsForDemo(Hotels hotels, Rooms rooms) {
        return args -> {
            if (hotels.count() > 0) return;
            Hotel harbor = hotels.save(new Hotel("North Pier House", "12 Canal Street", "Hamburg"));
            Hotel garden = hotels.save(new Hotel("Juniper Courtyard", "8 Maple Lane", "Berlin"));
            Hotel studio = hotels.save(new Hotel("Urban Nest", "30 Riverwalk", "Leipzig"));
            save(rooms, harbor, "A-101", 2, 5);
            save(rooms, harbor, "A-102", 2, 0);
            save(rooms, garden, "G-201", 1, 2);
            save(rooms, garden, "G-202", 3, 0);
            save(rooms, studio, "S-301", 2, 1);
            save(rooms, studio, "S-302", 4, 0);
        };
    }

    private void save(Rooms rooms, Hotel hotel, String number, int capacity, int count) {
        Room room = new Room(hotel, number, capacity);
        room.seedHistoricalCount(count);
        rooms.save(room);
    }
}
