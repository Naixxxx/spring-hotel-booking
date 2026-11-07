package io.staypilot.catalog.api;

import jakarta.validation.constraints.*;

public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record HotelWrite(@NotBlank String name, @NotBlank String address, @NotBlank String city) {
    }

    public record HotelPatch(String name, String address, String city) {
    }

    public record HotelView(Long id, String name, String address, String city, long rooms) {
    }

    public record RoomWrite(@NotNull Long hotelId, @NotBlank String number, @Min(1) @Max(12) int capacity) {
    }

    public record RoomPatch(String number, Boolean available, @Min(1) @Max(12) Integer capacity) {
    }

    public record HotelStatistics(Long hotelId, long rooms, long availableForSale, long activeReservations, long totalBookings) {
    }
}
