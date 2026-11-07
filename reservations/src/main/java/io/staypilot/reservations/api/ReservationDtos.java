package io.staypilot.reservations.api;

import io.staypilot.reservations.model.BookingStatus;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class ReservationDtos {

    private ReservationDtos() {
    }

    public record NewBooking(@NotNull UUID requestId, Long roomId, boolean autoSelect,
                             @NotNull LocalDate startDate, @NotNull LocalDate endDate) {
    }

    public record BookingView(UUID id, UUID requestId, Long userId, Long roomId, LocalDate startDate,
                              LocalDate endDate, BookingStatus status, boolean compensationPending,
                              String failureReason, Instant createdAt) {
    }

    public record Credentials(@NotBlank String username, @NotBlank String password) {
    }

    public record NewAccount(@NotBlank @Size(min = 3, max = 80) String username,
                             @NotBlank @Size(min = 8) String password) {
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
    }

    public record UserView(Long id, String username, String role) {
    }

    public record AdminUserWrite(@NotBlank String username, @NotBlank String password, @NotNull String role) {
    }

    public record AdminUserPatch(String password, String role) {
    }
}
