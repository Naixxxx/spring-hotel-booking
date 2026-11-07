package io.staypilot.contracts;

public record RoomCard(Long id, Long hotelId, String hotelName, String number,
                       boolean available, int timesBooked, int capacity) {
}
